# 生成测试用的 .torrent 文件（bencode 编码）
import os

OUT = r'Q:\AI\AndroidProjects\TorrentViewer\testdata'
os.makedirs(OUT, exist_ok=True)


def bencode(o):
    if isinstance(o, bool):
        o = int(o)
    if isinstance(o, int):
        return b'i%de' % o
    if isinstance(o, str):
        o = o.encode('utf-8')
    if isinstance(o, bytes):
        return b'%d:%s' % (len(o), o)
    if isinstance(o, list):
        return b'l' + b''.join(bencode(x) for x in o) + b'e'
    if isinstance(o, dict):
        items = sorted(o.items(), key=lambda kv: kv[0].encode('utf-8') if isinstance(kv[0], str) else kv[0])
        return b'd' + b''.join(bencode(k) + bencode(v) for k, v in items) + b'e'
    raise TypeError(type(o))


def write(name, obj):
    path = os.path.join(OUT, name)
    with open(path, 'wb') as f:
        f.write(bencode(obj))
    print(f'{name}: {os.path.getsize(path)} bytes')


# 1) 多文件种子（带 announce-list、中文名、多个文件）
multi = {
    'announce': 'https://tracker.example.com/announce',
    'announce-list': [
        ['https://tracker.example.com/announce'],
        ['udp://tracker2.example.com:6969/announce'],
    ],
    'comment': '这是一个用于测试的种子文件',
    'created by': 'TorrentViewer Test Suite',
    'creation date': 1762000000,
    'info': {
        'name': '测试种子合集',
        'piece length': 262144,
        'pieces': b'\x00' * 20 * 3,
        'files': [
            {'length': 1024 * 1024, 'path': ['视频', '第一集.mp4']},
            {'length': 2048 * 1024, 'path': ['视频', '第二集.mp4']},
            {'length': 512, 'path': ['说明.txt']},
        ],
    },
}
write('测试_多文件.torrent', multi)

# 2) 单文件种子
single = {
    'announce': 'udp://tracker.openbittorrent.com:6969/announce',
    'info': {
        'name': '单文件测试镜像.iso',
        'length': 700 * 1024 * 1024,
        'piece length': 524288,
        'pieces': b'\x01' * 20 * 1400,
    },
}
write('单文件测试.torrent', single)

# 3) 损坏文件（用于测试错误提示）
bad_path = os.path.join(OUT, '损坏文件.torrent')
with open(bad_path, 'wb') as f:
    f.write(b'this is not a valid torrent file at all')
print('损坏文件.torrent:', os.path.getsize(bad_path), 'bytes')
