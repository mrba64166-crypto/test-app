import sqlite3
import os
from flask import Flask, request

app = Flask(__name__)

# =========================================
# 🔴 1. تخزين بيانات حساسة بشكل غير آمن
# =========================================
DB_PASSWORD = "admin123"   # Hardcoded secret
API_KEY = "sk_test_ABC123" # Sensitive data in source code


# =========================================
# 🔴 2. حقن SQL (SQL Injection)
# =========================================
@app.route("/login")
def login():
    username = request.args.get("username")
    password = request.args.get("password")

    conn = sqlite3.connect("users.db")
    cursor = conn.cursor()

    # ❌ SQL Injection vulnerability
    query = f"SELECT * FROM users WHERE username = '{username}' AND password = '{password}'"
    cursor.execute(query)

    result = cursor.fetchone()
    if result:
        return "Login successful"
    else:
        return "Login failed"


# =========================================
# 🔴 3. كسر التحكم في الوصول (Broken Access Control)
# =========================================
@app.route("/admin")
def admin_panel():
    # ❌ No authentication / authorization check
    return "Welcome to Admin Panel"


# =========================================
# 🔴 4. حقن أوامر النظام (Command Injection)
# =========================================
@app.route("/ping")
def ping():
    host = request.args.get("host")

    # ❌ User input directly passed to OS command
    os.system("ping -c 1 " + host)

    return "Ping executed"


# =========================================
# 🔴 5. اجتياز المسارات (Path Traversal)
# =========================================
@app.route("/read-file")
def read_file():
    filename = request.args.get("file")

    # ❌ Path Traversal vulnerability
    with open(filename, "r") as f:
        return f.read()


# =========================================
# 🔴 6. فشل في تسجيل ومراقبة الأحداث الأمنية
# =========================================
@app.route("/transfer")
def transfer_money():
    amount = request.args.get("amount")
    to = request.args.get("to")

    # ❌ No logging, no monitoring, no alerts
    return f"Transferred {amount}$ to {to}"


if __name__ == "__main__":
    app.run(debug=True)
