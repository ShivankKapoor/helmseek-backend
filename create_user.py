#!/usr/bin/env python3
# /// script
# requires-python = ">=3.11"
# dependencies = [
#   "psycopg2-binary",
#   "argon2-cffi",
#   "python-dotenv",
# ]
# ///
"""Create a HelmSeek user directly in the database."""

import re
import sys
import getpass

try:
    import psycopg2
    from argon2 import PasswordHasher
    from dotenv import dotenv_values
except ImportError as e:
    print(f"Missing dependency: {e}")
    print("Run: pip install psycopg2-binary argon2-cffi python-dotenv")
    sys.exit(1)


def parse_jdbc_url(jdbc_url: str) -> dict:
    """Convert jdbc:postgresql://host:port/dbname to psycopg2 kwargs."""
    m = re.match(r"jdbc:postgresql://([^:/]+)(?::(\d+))?/(.+)", jdbc_url)
    if not m:
        raise ValueError(f"Cannot parse DB_URL: {jdbc_url}")
    return {
        "host": m.group(1),
        "port": int(m.group(2) or 5432),
        "dbname": m.group(3),
    }


def main():
    env = dotenv_values(".env")

    db_url = env.get("DB_URL", "")
    db_user = env.get("DB_USERNAME", "")
    db_pass = env.get("DB_PASSWORD", "")

    if not db_url:
        print("DB_URL not found in .env")
        sys.exit(1)

    conn_kwargs = parse_jdbc_url(db_url)
    conn_kwargs["user"] = db_user
    conn_kwargs["password"] = db_pass

    username = input("Username: ").strip().lower()
    if not username:
        print("Username cannot be empty.")
        sys.exit(1)

    password = getpass.getpass("Password: ")
    if not password:
        print("Password cannot be empty.")
        sys.exit(1)

    ph = PasswordHasher(
        time_cost=2,
        memory_cost=65536,
        parallelism=1,
        hash_len=32,
        salt_len=16,
    )
    hashed = ph.hash(password)

    try:
        conn = psycopg2.connect(**conn_kwargs)
        cur = conn.cursor()

        cur.execute("SELECT 1 FROM users WHERE LOWER(username) = %s", (username,))
        if cur.fetchone():
            print(f"User '{username}' already exists.")
            conn.close()
            sys.exit(1)

        cur.execute(
            "INSERT INTO users (username, password) VALUES (%s, %s)",
            (username, hashed),
        )
        conn.commit()
        print(f"User '{username}' created successfully.")
        conn.close()

    except psycopg2.OperationalError as e:
        print(f"Could not connect to database: {e}")
        sys.exit(1)


if __name__ == "__main__":
    main()
