המתן כמה שניות ואז חלץ את הכתובת מההפעלה החדשה:
```bash
invocation_id=$(sudo systemctl show cloudflared-quick.service -p InvocationID --value)

sudo journalctl "_SYSTEMD_INVOCATION_ID=$invocation_id" --no-pager \
  | grep -oE 'https://[a-zA-Z0-9-]+\.trycloudflare\.com' \
  | tail -n 1 \
  | sed 's|$|/vnc.html|'
```
אם עדיין אין פלט, הרץ שוב את הבלוק האחרון אחרי כמה שניות.
