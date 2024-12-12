#!/usr/bin/env python3
"""Check the running Docker Compose scenario without printing credentials."""
import json
import os
from pathlib import Path
import time
import urllib.error
import urllib.parse
import urllib.request

root = Path(__file__).resolve().parent.parent
credentials = {}
env_file = root / '.env'
if env_file.exists():
    for line in env_file.read_text().splitlines():
        if line and not line.lstrip().startswith('#'):
            key, separator, value = line.partition('=')
            if separator:
                credentials[key.strip()] = value.strip()
credentials.update(os.environ)
gateway = 'http://localhost:8888'
keycloak = 'http://localhost:8080/realms/cvs/protocol/openid-connect/token'


def request(url, token=None, data=None, method='GET', form=False):
    headers = {}
    if token:
        headers['Authorization'] = 'Bearer ' + token
    body = None
    if data is not None:
        body = (urllib.parse.urlencode(data) if form else json.dumps(data)).encode()
        headers['Content-Type'] = 'application/x-www-form-urlencoded' if form else 'application/json'
    req = urllib.request.Request(url, data=body, headers=headers, method=method)
    try:
        with urllib.request.urlopen(req, timeout=20) as response:
            return response.status, response.read(), dict(response.headers)
    except urllib.error.HTTPError as error:
        return error.code, error.read(), dict(error.headers)


def wait_for(url, token=None, timeout=120):
    deadline = time.monotonic() + timeout
    while time.monotonic() < deadline:
        try:
            result = request(url, token)
            if result[0] == 200:
                return result
        except (urllib.error.URLError, TimeoutError):
            pass
        time.sleep(1)
    raise SystemExit('Service did not become ready: ' + url)


def token(client, secret_name):
    status, body, _ = request(keycloak, data={
        'grant_type': 'client_credentials', 'client_id': client,
        'client_secret': credentials[secret_name],
    }, method='POST', form=True)
    if status != 200:
        raise SystemExit('Token request failed for ' + client + ': HTTP ' + str(status))
    return json.loads(body)['access_token']


wait_for(gateway + '/actuator/health')
reader = token('cvs-reader', 'CVS_READER_SECRET')
writer = token('cvs-writer', 'CVS_WRITER_SECRET')
assert request(gateway + '/cv/1')[0] == 401, 'Missing-token request must return 401'
status, body, _ = wait_for(gateway + '/cv/1', reader)
assert json.loads(body)['countryName'] == 'Russia', 'Feign must return the country name'
cv = {'name': 'Alex', 'surname': 'Ivanov', 'countryName': 'Russia', 'city': 'Moscow', 'status': 'DRAFT'}
assert request(gateway + '/cv', reader, cv, 'POST')[0] == 403, 'Reader must not create a CV'
status, body, headers = request(gateway + '/cv', writer, cv, 'POST')
assert status == 201 and json.loads(body)['uuid'], 'Writer must create a CV'
assert any(key.lower() == 'location' for key in headers), 'Created CV must have a Location header'
assert request(gateway + '/countries/1/events', writer, method='POST')[0] == 202, 'Event publication must be acknowledged'
_, body, _ = wait_for(gateway + '/cv/country-events/1', reader, timeout=30)
assert json.loads(body) == {'countryId': 1, 'countryName': 'Russia'}, 'Consumer must update the event projection'
print('Scenario passed: authentication, roles, Feign, CV creation and Kafka event consumption.')
