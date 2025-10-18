from fastapi.testclient import TestClient
from test_corr import app

client = TestClient(app)

def test_auto_generate_id():
    r = client.get("/ping")
    assert r.status_code == 200
    assert "X-Correlation-ID" in r.headers
    assert r.json()["correlation_id"] == r.headers["X-Correlation-ID"]

def test_propagate_header():
    r = client.get("/ping", headers={"X-Correlation-ID": "demo-123"})
    assert r.status_code == 200
    assert r.headers["X-Correlation-ID"] == "demo-123"
    assert r.json()["correlation_id"] == "demo-123"
