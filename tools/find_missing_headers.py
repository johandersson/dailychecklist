import glob
files=glob.glob('src/main/java/**/*.java',recursive=True)
missing=[]
for f in sorted(files):
    with open(f,'r',encoding='utf-8') as fh:
        first=fh.readline()
        if not first.strip().startswith('/*'):
            missing.append(f)
for m in missing:
    print(m)
print('COUNT:'+str(len(missing)))
