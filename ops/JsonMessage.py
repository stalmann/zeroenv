import json
from zeroops.Files import fread

message = fread("../../data/testdoc.json")
print(message)
jsondoc = json.loads(message)
print(json.dumps(jsondoc))
