import { test } from 'node:test';
import assert from 'node:assert/strict';
import crypto from 'node:crypto';
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import { spawn } from 'node:child_process';
import { fileURLToPath } from 'node:url';
import { boundPort } from './helpers.mjs';

test('only an authenticated bearer session bypasses the browser origin check', async t => {
  const data = fs.mkdtempSync(path.join(os.tmpdir(), 'opengym-csrf-'));
  const secret = crypto.randomBytes(32).toString('hex');
  fs.writeFileSync(path.join(data, 'secret'), secret, { mode: 0o600 });
  fs.writeFileSync(path.join(data, 'db.json'), JSON.stringify({
    users: [{ id: 'fixture-user', name: 'Fixture' }], creds: [], subs: [], invites: []
  }));
  const child = spawn(process.execPath, ['server.js'], {
    cwd: fileURLToPath(new URL('../', import.meta.url)),
    stdio: ['ignore', 'pipe', 'pipe'],
    env: { ...process.env, DATA_DIR: data, PORT: '0', ORIGIN: 'https://gym.example.test', COACH_DISABLED: '1' }
  });
  let log = '';
  child.stdout.on('data', chunk => { log += chunk; });
  child.stderr.on('data', chunk => { log += chunk; });
  t.after(() => { child.kill('SIGKILL'); fs.rmSync(data, { recursive: true, force: true }); });
  const port = await boundPort(child, () => log);
  const payload = `fixture-user:${Date.now() + 60000}:0`;
  const token = payload + '.' + crypto.createHmac('sha256', secret).update(payload).digest('base64url');
  const cookie = `__Host-gymsid=${token}`;
  const send = headers => fetch(`http://127.0.0.1:${port}/api/data`, {
    method: 'PUT', headers: { Origin: 'https://other.example.test', 'Content-Type': 'application/json', ...headers },
    body: JSON.stringify({ state: { workouts: [], routines: [] } })
  });
  assert.equal((await send({ Cookie: cookie })).status, 403);
  assert.equal((await send({ Cookie: cookie, Authorization: 'Bearer invalid' })).status, 403);
  assert.equal((await send({ Cookie: cookie, Authorization: `Bearer ${token}` })).status, 403);
  assert.equal((await send({ Authorization: 'Bearer invalid' })).status, 403);
  assert.equal((await send({ Authorization: `Bearer ${token}` })).status, 200);
  assert.ok(fs.existsSync(path.join(data, 'state-fixture-user.json')));
});
