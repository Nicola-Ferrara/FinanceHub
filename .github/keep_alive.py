import psycopg2
import os

# Leggi le variabili d'ambiente (impostate nei Secrets di GitHub)
DB_HOST = os.getenv("DB_HOST")
DB_PORT = os.getenv("DB_PORT", "5432")
DB_NAME = os.getenv("DB_NAME")
DB_USER = os.getenv("DB_USER")
DB_PASSWORD = os.getenv("DB_PASSWORD")

try:
    print("🔄 Connessione al database Supabase...")
    conn = psycopg2.connect(
        host=DB_HOST,
        port=DB_PORT,
        dbname=DB_NAME,
        user=DB_USER,
        password=DB_PASSWORD
    )
    cur = conn.cursor()
    cur.execute("SELECT 1;")
    result = cur.fetchone()
    print("✅ Query eseguita con successo:", result)

    cur.close()
    conn.close()
    print("🔌 Connessione chiusa.")
except Exception as e:
    print("❌ Errore durante la connessione:", e)