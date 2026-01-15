import os
import sqlite3
import logging
import subprocess
from flask import Flask, request, abort

app = Flask(__name__)

# =========================================
# ✅ 1. إعداد Logging (مراقبة الأحداث الأمنية)
# =========================================
logging.basicConfig(
    filename="security.log",
    level=logging.INFO,
    format="%(asctime)s - %(levelname)s - %(message)s"
)

# =========================================
# ✅ 2. بيانات حساسة من متغيرات البيئة
# =========================================
DB_PASSWORD = os.getenv("DB_PASSWORD")
API_KEY = os.getenv("API_KEY")

if not DB_PASSWORD or not API_KEY:
    raise RuntimeError("Missing environment variables")

# =========================================
# ✅ 3. SQL Injection Protection
# =========================================
@app.route("/login")
def login():
    username = request.args.get("username")
    password = request.args.get("password")

    conn = sqlite3.connect("users.db")
    cursor = conn.cursor()

    query = "SELECT * FROM users WHERE username = ? AND password = ?"
    cursor.execute(query, (username, password))

    result = cursor.fetchone()

    if result:
        logging.info(f"Successful login for user: {username}")
        return "Login successful"
    else:
        logging.warning(f"Failed login attempt for user: {username}")
        return "Login failed"


# =========================================
# ✅ 4. كسر التحكم في الوصول (Authorization)
# =========================================
def is_admin(request):
    role = request.headers.get("X-ROLE")
    return role == "admin"


@app.route("/admin")
def admin_panel():
    if not is_admin(request):
        logging.warning("Unauthorized access attempt to admin panel")
        abort(403)

    return "Welcome to Admin Panel"


# =========================================
# ✅ 5. منع Command Injection
# =========================================
@app.route("/ping")
def ping():
    host = request.args.get("host")

    # Whitelist validation
    allowed_hosts = ["127.0.0.1", "localhost"]
    if host not in allowed_hosts:
        logging.warning(f"Blocked ping attempt to: {host}")
        abort(400)

    subprocess.run(
        ["ping", "-c", "1", host],
        capture_output=True,
        text=True,
        check=True
    )

    return "Ping executed safely"


# =========================================
# ✅ 6. منع Path Traversal
# =========================================
BASE_DIR = os.path.abspath("safe_files")


@app.route("/read-file")
def read_file():
    filename = request.args.get("file")
    requested_path = os.path.abspath(os.path.join(BASE_DIR, filename))

    if not requested_path.startswith(BASE_DIR):
        logging.warning("Path traversal attempt detected")
        abort(403)

    if not os.path.exists(requested_path):
        abort(404)

    with open(requested_path, "r") as f:
        return f.read()


# =========================================
# ✅ 7. Logging للأحداث الحساسة
# =========================================
@app.route("/transfer")
def transfer_money():
    amount = request.args.get("amount")
    to = request.args.get("to")

    logging.info(f"Money transfer requested: {amount}$ to {to}")

    return "Transfer completed securely"


if __name__ == "__main__":
    app.run(debug=False)
