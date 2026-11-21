"""Generate the allowed resource names from local AAPT2 and JADX outputs."""
import argparse
import json
import re
from pathlib import Path

parser = argparse.ArgumentParser()
parser.add_argument("resources", type=Path)
parser.add_argument("icon_map", type=Path)
parser.add_argument("build")
parser.add_argument("model")
parser.add_argument("output", type=Path)
args = parser.parse_args()
resources = dict((name, identifier) for identifier, name in re.findall(
    r"^    resource (0x[0-9a-f]+) drawable/(\w+)\s*$",
    args.resources.read_text(encoding="utf-8-sig"), re.M))
targets = set(re.findall(r'TuplesKt\.to\("(\w+)"', args.icon_map.read_text(encoding="utf-8")))
entries = {name: resources[name] for name in sorted(targets & resources.keys())}
assert "stat_sys_wifi_signal_4" in entries
args.output.write_text(json.dumps(dict(model=args.model, build=args.build, resources=entries),
                                  indent=2) + "\n", encoding="utf-8")
print(len(entries), "correspondances sur", len(targets))
