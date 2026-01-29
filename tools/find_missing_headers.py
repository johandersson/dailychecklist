import glob
files=glob.glob('src/main/java/**/*.java',recursive=True)
missing=[]
for f in sorted(files):
    try:
        with open(f,'r',encoding='utf-8') as fh:
            content = fh.read(4096)
            if 'Copyright (C)' not in content:
                missing.append(f)
    except Exception as e:
        print('ERR', f, e)
for m in missing:
    print(m)
print('COUNT:'+str(len(missing)))
