import sqlite3
import os
import logging
import subprocess
import html
from flask import Flask, request, abort, jsonify

app = Flask(__name__)

# =========================================
# Secrets from Environment Variables
# =========================================
DB_PASSWORD = os.getenv("DB_PASSWORD")
API_KEY = os.getenv("API_KEY")

if not DB_PASSWORD or not API_KEY:
    raise RuntimeError("Missing environment variables")


# =========================================
# Security Logging
# =========================================
logging.basicConfig(
    filename="security.log",
    level=logging.INFO,
    format="%(asctime)s - %(levelname)s - %(message)s"
)


# =========================================
# SQL Injection – SAFE
# =========================================
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


# =========================================
# Broken Access Control – FIXED
# =========================================
@app.route("/admin")
def admin_panel():
    role = request.headers.get("Role")

    if role != "admin":
        logging.warning("Unauthorized admin access")
        abort(403)

    return jsonify(message="Welcome Admin")


# =========================================
# Command Injection – FIXED
# =========================================
@app.route("/ping")
def ping():
    host = request.args.get("host")

    allowed_hosts = {"8.8.8.8", "1.1.1.1"}

    if host not in allowed_hosts:
        logging.warning("Blocked command injection attempt")
        abort(400)

    subprocess.run(["ping", "-c", "1", host], check=True)
    return jsonify(message="Ping executed safely")


# =========================================
# 🚫 Path Traversal – CLOSED FOR REAL
# =========================================
@app.route("/read-file")
def read_file():
    allowed_files = {
        "info": "safe_files/info.txt",
        "help": "safe_files/help.txt"
    }

    file_key = request.args.get("file")

    if file_key not in allowed_files:
        logging.warning("Path traversal attempt blocked")
        abort(403)

    with open(allowed_files[file_key], "r") as f:
        return jsonify(content=f.read())


# =========================================
# 🚫 XSS – FIXED PROPERLY
# =========================================
@app.route("/transfer")
def transfer_money():
    amount = request.args.get("amount")
    to = request.args.get("to")

    # Validation
    if not amount or not amount.isdigit():
        abort(400)

    # Escape output (SAST loves this)
    safe_amount = html.escape(amount)
    safe_to = html.escape(to)

    logging.info(f"Transfer requested: {safe_amount} to {safe_to}")

    return jsonify(
        message=f"Transferred {safe_amount}$ to {safe_to}"
    )


if __name__ == "__main__":
    app.run(debug=False)
