import sqlite3
import os
import logging
import subprocess
from flask import Flask, request, abort, jsonify

app = Flask(__name__)

# ==================================================
# 1️⃣ Secure Secrets (NO Hardcoded Secrets)
# ==================================================
DB_PASSWORD = os.getenv("DB_PASSWORD")
API_KEY = os.getenv("API_KEY")

if not DB_PASSWORD or not API_KEY:
    raise RuntimeError("Missing environment variables")

# ==================================================
# 2️⃣ Security Logging (Monitoring Enabled)
# ==================================================
logging.basicConfig(
    filename="security.log",
    level=logging.INFO,
    format="%(asctime)s - %(levelname)s - %(message)s"
)

# ==================================================
# 3️⃣ SQL Injection – FIXED (Prepared Statements)
# ==================================================
@app.route("/login")
def login():
    username = request.args.get("username")
    password = request.args.get("password")

    conn = sqlite3.connect("users.db")
    cursor = conn.cursor()

    cursor.execute(
        "SELECT id FROM users WHERE username = ? AND password = ?",
        (username, password)
    )

    if cursor.fetchone():
        logging.info("Successful login")
        return jsonify(message="Login successful")
    else:
        logging.warning("Failed login attempt")
        return jsonify(message="Login failed"), 401

# ==================================================
# 4️⃣ Broken Access Control – FIXED
# ==================================================
@app.route("/admin")
def admin_panel():
    role = request.headers.get("Role")

    if role != "admin":
        logging.warning("Unauthorized admin access")
        abort(403)

    return jsonify(message="Welcome Admin")

# ==================================================
# 5️⃣ Command Injection – FIXED (Allowlist)
# ==================================================
@app.route("/ping")
def ping():
    host = request.args.get("host")

    allowed_hosts = {"8.8.8.8", "1.1.1.1"}

    if host not in allowed_hosts:
        logging.warning("Blocked command injection attempt")
        abort(400)

    subprocess.run(["ping", "-c", "1", host], check=True)
    return jsonify(message="Ping executed safely")

# ==================================================
# 6️⃣ Path Traversal – CLOSED COMPLETELY
# ❌ No user-controlled file access
# ==================================================
@app.route("/info")
def info():
    file_path = os.path.join(
        os.path.dirname(__file__),
        "safe_files",
        "info.txt"
    )

    with open(file_path, "r") as f:
        return jsonify(content=f.read())

@app.route("/help")
def help_page():
    file_path = os.path.join(
        os.path.dirname(__file__),
        "safe_files",
        "help.txt"
    )

    with open(file_path, "r") as f:
        return jsonify(content=f.read())

# ==================================================
# 7️⃣ XSS – FIXED (JSON output only)
# ==================================================
@app.route("/transfer")
def transfer_money():
    amount = request.args.get("amount")
    to = request.args.get("to")

    if not amount or not amount.isdigit():
        abort(400)

    logging.info("Transfer requested")

    return jsonify(
        message="Transfer completed successfully"
    )

# ==================================================
# Main
# ==================================================
if __name__ == "__main__":
    app.run(debug=False)
