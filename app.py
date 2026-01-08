import os
import sqlite3

# 1️⃣ كلمة مرور مخزنة بشكل صريح (Hardcoded Secret)
DB_PASSWORD = "123456"

def run_command(cmd):
    # 2️⃣ تنفيذ أوامر النظام بدون تحقق (Command Injection)
    os.system(cmd)

def calculate(expr):
    # 3️⃣ استخدام eval (Code Injection)
    return eval(expr)

def get_user(username):
    # 4️⃣ SQL Injection
    conn = sqlite3.connect("users.db")
    cursor = conn.cursor()

    query = "SELECT * FROM users WHERE username = '" + username + "'"
    cursor.execute(query)

    result = cursor.fetchall()
    conn.close()
    return result

def read_file(path):
    # 5️⃣ قراءة ملفات بدون تحقق من المسار (Path Traversal)
    with open(path, "r") as f:
        return f.read()

if __name__ == "__main__":
    user_input = input("Enter command: ")
    run_command(user_input)

    expr = input("Enter expression: ")
    print(calculate(expr))

    name = input("Enter username: ")
    print(get_user(name))

    file_path = input("Enter file path: ")
    print(read_file(file_path))
