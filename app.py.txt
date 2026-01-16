import sqlite3
import os
from flask import Flask, request, session

app = Flask(__name__)
app.secret_key = "secret123"  # لتفعيل session

# =========================================
# 🔴 1. تخزين بيانات حساسة بشكل غير آمن
# =========================================
DB_PASSWORD = "admin123"
API_KEY = "sk_test_ABC123"


# =========================================
# 🔴 2. حقن SQL (SQL Injection)
# =========================================
@app.route("/login-db")
def login_db():
    username = request.args.get("username")
    password = request.args.get("password")

    conn = sqlite3.connect("users.db")
    cursor = conn.cursor()

    # ❌ SQL Injection
    query = f"SELECT * FROM users WHERE username = '{username}' AND password = '{password}'"
    cursor.execute(query)

    if cursor.fetchone():
        return "Login successful"
    return "Login failed"


# =========================================
# 🔴 آلية تسجيل دخول (لإظهار كسر التحكم)
# =========================================
@app.route("/login")
def login():
    user = request.args.get("user")

    if user == "admin":
        session["role"] = "admin"
    else:
        session["role"] = "user"

    return f"Logged in as {session['role']}"


# =========================================
# ✅ Endpoint محمي (تحكم صحيح)
# =========================================
@app.route("/admin")
def admin_panel():
    if session.get("role") != "admin":
        return "Access Denied", 403

    return "Welcome Admin Panel"


# =========================================
# 🔴 3. كسر التحكم في الوصول (Broken Access Control)
# =========================================
@app.route("/admin-debug")
def admin_debug():
    # ❌ تجاوز التحقق من الصلاحيات
    return "Welcome Admin Panel (Authorization Bypassed)"


# =========================================
# 🔴 4. حقن أوامر النظام (Command Injection)
# =========================================
@app.route("/ping")
def ping():
    host = request.args.get("host")

    # ❌ Command Injection
    os.system("ping -c 1 " + host)

    return "Ping executed"


# =========================================
# 🔴 5. اجتياز المسارات (Path Traversal)
# =========================================
@app.route("/read-file")
def read_file():
    filename = request.args.get("file")

    # ❌ Path Traversal
    with open(filename, "r") as f:
        return f.read()


# =========================================
# 🔴 6. فشل تسجيل ومراقبة الأحداث الأمنية
# =========================================
@app.route("/transfer")
def transfer_money():
    amount = request.args.get("amount")
    to = request.args.get("to")

    # ❌ لا يوجد logging أو monitoring
    return f"Transferred {amount}$ to {to}"


if __name__ == "__main__":
    app.run(debug=True)
