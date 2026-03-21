#!/usr/bin/env python3
"""Convert IDEA_BUDDY_REPORT.md to PDF using markdown + weasyprint."""
import markdown
from weasyprint import HTML
from pathlib import Path

md_path = Path(__file__).parent / "IDEA_BUDDY_REPORT.md"
pdf_path = Path(__file__).parent / "IDEA_BUDDY_REPORT.pdf"

md_content = md_path.read_text()

html_body = markdown.markdown(md_content, extensions=["tables", "fenced_code"])

html_doc = f"""<!DOCTYPE html>
<html>
<head>
<meta charset="utf-8">
<style>
    @page {{
        size: A4;
        margin: 2cm;
    }}
    body {{
        font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Helvetica, Arial, sans-serif;
        font-size: 11pt;
        line-height: 1.5;
        color: #1a1a1a;
        max-width: 100%;
    }}
    h2 {{
        color: #0d1117;
        border-bottom: 2px solid #d0d7de;
        padding-bottom: 0.3em;
        margin-top: 1.5em;
        font-size: 1.5em;
    }}
    h3 {{
        color: #1f2328;
        margin-top: 1.2em;
        font-size: 1.2em;
    }}
    table {{
        border-collapse: collapse;
        width: 100%;
        margin: 1em 0;
        font-size: 10pt;
    }}
    th, td {{
        border: 1px solid #d0d7de;
        padding: 8px 12px;
        text-align: left;
    }}
    th {{
        background-color: #f6f8fa;
        font-weight: 600;
    }}
    tr:nth-child(even) {{
        background-color: #f9fafb;
    }}
    strong {{
        color: #0d1117;
    }}
    a {{
        color: #0969da;
        text-decoration: none;
    }}
    ul {{
        padding-left: 1.5em;
    }}
    li {{
        margin-bottom: 0.3em;
    }}
    p {{
        margin: 0.5em 0;
    }}
    code {{
        background-color: #f6f8fa;
        padding: 0.2em 0.4em;
        border-radius: 3px;
        font-size: 0.9em;
    }}
</style>
</head>
<body>
{html_body}
</body>
</html>"""

HTML(string=html_doc).write_pdf(str(pdf_path))
print(f"PDF generated: {pdf_path}")
