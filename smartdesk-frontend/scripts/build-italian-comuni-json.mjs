import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const root = path.resolve(__dirname, '..');
const rawPath = path.join(root, 'public', 'comuni-raw.json');
const outPath = path.join(root, 'public', 'data', 'italian-comuni.json');

const raw = JSON.parse(fs.readFileSync(rawPath, 'utf8'));
const counts = new Map();
for (const c of raw) counts.set(c.nome, (counts.get(c.nome) ?? 0) + 1);
const out = raw
  .map((c) => {
    const dup = (counts.get(c.nome) ?? 0) > 1;
    const label = dup ? `${c.nome} (${c.sigla})` : c.nome;
    return { label, n: c.nome, s: c.sigla };
  })
  .sort((a, b) => a.label.localeCompare(b.label, 'it'));

fs.mkdirSync(path.dirname(outPath), { recursive: true });
fs.writeFileSync(outPath, JSON.stringify(out));
console.log('Comuni:', out.length, '→', outPath, `(${fs.statSync(outPath).size} bytes)`);
