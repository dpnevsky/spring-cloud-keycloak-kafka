#!/usr/bin/env python3
"""Create local credentials without overwriting an existing .env."""
import base64
import os
from pathlib import Path
import secrets

root = Path(__file__).resolve().parent.parent
target = root / '.env'
values = {
    'KEYCLOAK_ADMIN': 'admin',
    'KEYCLOAK_ADMIN_PASSWORD': secrets.token_hex(24),
    'CVS_READER_SECRET': secrets.token_hex(32),
    'CVS_WRITER_SECRET': secrets.token_hex(32),
    'JWT_SECRET': base64.b64encode(secrets.token_bytes(32)).decode(),
}
try:
    fd = os.open(target, os.O_WRONLY | os.O_CREAT | os.O_EXCL, 0o600)
except FileExistsError:
    raise SystemExit('.env already exists; it was left unchanged.')
with os.fdopen(fd, 'w') as output:
    output.write(''.join(f'{key}={value}\n' for key, value in values.items()))
print('Created .env with local credentials.')
