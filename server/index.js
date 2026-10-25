const express = require('express');
const cors = require('cors');
const Busboy = require('busboy');
const path = require('path');
const { MongoClient, GridFSBucket, ObjectId } = require('mongodb');

const PORT = process.env.PORT || 8000;
const MONGODB_URL = process.env.MONGODB_URL || process.env.MONGODB_URI;
const ADMIN_KEY = process.env.ADMIN_KEY;
const CUSTOM_ID_START = 1000000; // server ke apne anime IDs (AniList IDs se alag)

if (!MONGODB_URL) { console.error('MONGODB_URL env missing'); process.exit(1); }
if (!ADMIN_KEY) { console.error('ADMIN_KEY env missing'); process.exit(1); }

const app = express();
app.set('trust proxy', true);
app.use(cors());
app.use(express.json({ limit: '2mb' }));

let db, bucket, animeCol, episodesCol, countersCol;

const baseUrl = (req) => (process.env.PUBLIC_URL || `${req.protocol}://${req.get('host')}`).replace(/\/+$/, '');
const absolute = (req, u) => (u && u.startsWith('/') ? baseUrl(req) + u : u || '');

function requireAdmin(req, res, next) {
  if (req.get('x-admin-key') !== ADMIN_KEY) return res.status(401).json({ error: 'Invalid admin key' });
  next();
}

async function nextAnimeId() {
  const r = await countersCol.findOneAndUpdate(
    { _id: 'anime' }, { $inc: { seq: 1 } }, { upsert: true, returnDocument: 'after' });
  return CUSTOM_ID_START + (r.seq || r.value?.seq || 0);
}

const animeOut = (req, a) => ({
  id: a.id, title: a.title, englishTitle: a.englishTitle || null,
  coverImage: absolute(req, a.coverImage), bannerImage: absolute(req, a.bannerImage || a.coverImage),
  averageScore: a.averageScore || 0, format: a.format || 'TV', episodes: a.episodes || 0,
  durationMinutes: a.durationMinutes || 24, seasonYear: a.seasonYear || null,
  status: a.status || 'RELEASING', genres: a.genres || [], description: a.description || '',
  studio: a.studio || null
});

const episodeOut = (req, e) => ({
  animeId: e.animeId, number: e.number, title: e.title || `Episode ${e.number}`,
  videoUrl: absolute(req, e.videoUrl), type: e.type || guessType(e.videoUrl),
  thumbnail: absolute(req, e.thumbnail), durationMinutes: e.durationMinutes || 24
});

function guessType(u = '') { return /\.m3u8(\?|$)/i.test(u) ? 'm3u8' : 'mp4'; }

// ---------- Public API (app use karta hai) ----------
app.get('/api/health', (req, res) => res.json({ ok: true }));

app.get('/api/anime', async (req, res) => {
  const q = (req.query.q || '').toString().trim();
  const filter = q ? { title: { $regex: q.replace(/[.*+?^${}()|[\]\\]/g, '\\$&'), $options: 'i' } } : {};
  const list = await animeCol.find(filter).sort({ createdAt: -1 }).limit(100).toArray();
  res.json(list.map((a) => animeOut(req, a)));
});

app.get('/api/anime/:id', async (req, res) => {
  const a = await animeCol.findOne({ id: Number(req.params.id) });
  if (!a) return res.status(404).json({ error: 'Not found' });
  res.json(animeOut(req, a));
});

app.get('/api/anime/:id/episodes', async (req, res) => {
  const list = await episodesCol.find({ animeId: Number(req.params.id) }).sort({ number: 1 }).toArray();
  res.json(list.map((e) => episodeOut(req, e)));
});

// Player yahan se stream URL leta hai (AniList anime ke liye bhi chalta hai)
app.get('/api/stream/:animeId/:episode', async (req, res) => {
  const e = await episodesCol.findOne({ animeId: Number(req.params.animeId), number: Number(req.params.episode) });
  if (!e || !e.videoUrl) return res.status(404).json({ error: 'No stream' });
  res.json(episodeOut(req, e));
});

// ---------- Admin API ----------
app.post('/api/anime', requireAdmin, async (req, res) => {
  const b = req.body || {};
  if (!b.title) return res.status(400).json({ error: 'title required' });
  const doc = {
    id: await nextAnimeId(), title: b.title, englishTitle: b.englishTitle || null,
    coverImage: b.coverImage || '', bannerImage: b.bannerImage || '',
    averageScore: Number(b.averageScore) || 0, format: b.format || 'TV',
    episodes: Number(b.episodes) || 0, durationMinutes: Number(b.durationMinutes) || 24,
    seasonYear: b.seasonYear ? Number(b.seasonYear) : null, status: b.status || 'RELEASING',
    genres: Array.isArray(b.genres) ? b.genres : String(b.genres || '').split(',').map((s) => s.trim()).filter(Boolean),
    description: b.description || '', studio: b.studio || null, createdAt: new Date()
  };
  await animeCol.insertOne(doc);
  res.json(animeOut(req, doc));
});

app.put('/api/anime/:id', requireAdmin, async (req, res) => {
  const allowed = ['title', 'englishTitle', 'coverImage', 'bannerImage', 'averageScore', 'format', 'episodes',
    'durationMinutes', 'seasonYear', 'status', 'description', 'studio'];
  const set = {};
  for (const k of allowed) if (k in req.body) set[k] = req.body[k];
  if ('genres' in req.body) set.genres = Array.isArray(req.body.genres)
    ? req.body.genres : String(req.body.genres).split(',').map((s) => s.trim()).filter(Boolean);
  await animeCol.updateOne({ id: Number(req.params.id) }, { $set: set });
  res.json({ ok: true });
});

app.delete('/api/anime/:id', requireAdmin, async (req, res) => {
  const id = Number(req.params.id);
  await animeCol.deleteOne({ id });
  await episodesCol.deleteMany({ animeId: id });
  res.json({ ok: true });
});

// Episode add/update. animeId AniList ka bhi ho sakta hai ya server ka (1000000+)
app.post('/api/anime/:id/episodes', requireAdmin, async (req, res) => {
  const animeId = Number(req.params.id);
  const b = req.body || {};
  const number = Number(b.number);
  if (!number || !b.videoUrl) return res.status(400).json({ error: 'number and videoUrl required' });
  const doc = {
    animeId, number, title: b.title || `Episode ${number}`, videoUrl: b.videoUrl,
    type: b.type || guessType(b.videoUrl), thumbnail: b.thumbnail || '',
    durationMinutes: Number(b.durationMinutes) || 24
  };
  await episodesCol.updateOne({ animeId, number }, { $set: doc }, { upsert: true });
  res.json(episodeOut(req, doc));
});

app.delete('/api/anime/:id/episodes/:number', requireAdmin, async (req, res) => {
  await episodesCol.deleteOne({ animeId: Number(req.params.id), number: Number(req.params.number) });
  res.json({ ok: true });
});

// ---------- Upload (photo / video / subtitle) -> MongoDB GridFS ----------
app.post('/api/upload', requireAdmin, (req, res) => {
  let bb;
  try { bb = Busboy({ headers: req.headers, limits: { files: 1 } }); }
  catch { return res.status(400).json({ error: 'multipart/form-data required' }); }
  let done = false;
  const finish = (code, body) => { if (!done) { done = true; res.status(code).json(body); } };

  bb.on('file', (field, file, info) => {
    const { filename, mimeType } = info;
    const up = bucket.openUploadStream(filename || 'file', { metadata: { contentType: mimeType } });
    file.pipe(up);
    up.on('finish', () => finish(200, {
      id: up.id.toString(), name: filename, contentType: mimeType,
      url: `${baseUrl(req)}/files/${up.id}`
    }));
    up.on('error', (e) => finish(500, { error: e.message }));
  });
  bb.on('error', (e) => finish(400, { error: e.message }));
  bb.on('close', () => setTimeout(() => finish(400, { error: 'No file received' }), 50));
  req.pipe(bb);
});

app.get('/api/files', requireAdmin, async (req, res) => {
  const files = await bucket.find().sort({ uploadDate: -1 }).limit(200).toArray();
  res.json(files.map((f) => ({
    id: f._id.toString(), name: f.filename, size: f.length, contentType: f.metadata?.contentType,
    url: `${baseUrl(req)}/files/${f._id}`, uploadedAt: f.uploadDate
  })));
});

app.delete('/api/files/:id', requireAdmin, async (req, res) => {
  try { await bucket.delete(new ObjectId(req.params.id)); res.json({ ok: true }); }
  catch (e) { res.status(404).json({ error: 'Not found' }); }
});

// Public file stream, Range support (video seek ke liye zaroori)
app.get('/files/:id', async (req, res) => {
  let id;
  try { id = new ObjectId(req.params.id); } catch { return res.status(404).end(); }
  const f = await db.collection('fs.files').findOne({ _id: id });
  if (!f) return res.status(404).end();

  const type = f.metadata?.contentType || 'application/octet-stream';
  const size = f.length;
  res.set({ 'Accept-Ranges': 'bytes', 'Content-Type': type, 'Cache-Control': 'public, max-age=86400' });

  const range = req.headers.range;
  if (range) {
    const m = /bytes=(\d*)-(\d*)/.exec(range);
    let start = m && m[1] ? parseInt(m[1], 10) : 0;
    let end = m && m[2] ? parseInt(m[2], 10) : size - 1;
    if (start >= size || end >= size || start > end) {
      res.status(416).set('Content-Range', `bytes */${size}`); return res.end();
    }
    res.status(206).set({ 'Content-Range': `bytes ${start}-${end}/${size}`, 'Content-Length': end - start + 1 });
    bucket.openDownloadStream(id, { start, end: end + 1 }).on('error', () => res.end()).pipe(res);
  } else {
    res.set('Content-Length', size);
    bucket.openDownloadStream(id).on('error', () => res.end()).pipe(res);
  }
});

// Admin panel (phone browser me /admin kholo)
app.use('/admin', express.static(path.join(__dirname, 'public')));
app.get('/', (req, res) => res.json({ name: 'AnimeXZ server', admin: '/admin', health: '/api/health' }));

app.use((err, req, res, next) => { console.error(err); res.status(500).json({ error: 'Server error' }); });

(async () => {
  const client = new MongoClient(MONGODB_URL);
  await client.connect();
  db = client.db(); // database name URL se aata hai
  bucket = new GridFSBucket(db);
  animeCol = db.collection('anime');
  episodesCol = db.collection('episodes');
  countersCol = db.collection('counters');
  await animeCol.createIndex({ id: 1 }, { unique: true });
  await episodesCol.createIndex({ animeId: 1, number: 1 }, { unique: true });
  app.listen(PORT, () => console.log('AnimeXZ server on :' + PORT));
})().catch((e) => { console.error(e); process.exit(1); });
