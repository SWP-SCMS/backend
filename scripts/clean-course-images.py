"""Run during maintenance, using a fresh JSON export of courses.image_url."""
import argparse
import json
import re
import time
from pathlib import Path

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument("directory", type=Path)
parser.add_argument("references", type=Path, help="JSON array of all current courses.image_url values")
parser.add_argument("--delete", action="store_true", help="Delete candidates; stop application writes first")
args = parser.parse_args()
references = json.loads(args.references.read_text(encoding="utf-8"))
if not isinstance(references, list) or any(x is not None and not isinstance(x, str) for x in references):
    parser.error("references must be a JSON array of strings or nulls")
referenced_names = {x.rsplit("/", 1)[-1] for x in references if x}
directory = args.directory.resolve(strict=True)
cutoff = time.time() - 7 * 86400
for path in sorted(directory.iterdir()):
    if path.is_symlink() or not path.is_file():
        continue
    if not re.fullmatch(r"[0-9a-fA-F-]{36}\.(jpg|png)", path.name):
        continue
    if path.name in referenced_names or path.stat().st_mtime >= cutoff:
        continue
    print(("DELETE " if args.delete else "CANDIDATE ") + str(path))
    if args.delete:
        path.unlink()
