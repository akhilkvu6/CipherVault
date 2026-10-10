"""
Styles and HTML page scaffolding for CipherVault Master Project Report.
Matches CipherVault's warm heritage visual identity (#815621, #FEDDBD, #FFF8F4).
"""

CSS_STYLES = '''
  @page {
    size: 210mm 297mm;
    margin: 0;
  }
  * {
    box-sizing: border-box;
    -webkit-print-color-adjust: exact !important;
    print-color-adjust: exact !important;
  }
  body {
    margin: 0;
    padding: 0;
    font-family: 'Segoe UI', -apple-system, BlinkMacSystemFont, Roboto, Helvetica, Arial, sans-serif;
    color: #2D241E;
    background-color: #FFFFFF;
    font-size: 8.8pt;
    line-height: 1.45;
  }
  .page {
    width: 210mm;
    height: 297mm;
    padding: 22mm 18mm 18mm 18mm;
    page-break-after: always;
    page-break-inside: avoid;
    position: relative;
    overflow: hidden;
    background: #FFFFFF;
  }
  .page-header {
    position: absolute;
    top: 8mm;
    left: 18mm;
    right: 18mm;
    height: 7mm;
    display: flex;
    justify-content: space-between;
    align-items: center;
    font-size: 7.8pt;
    color: #815621;
    border-bottom: 1px solid #E6D7CD;
    text-transform: uppercase;
    letter-spacing: 0.6px;
    font-weight: 600;
  }
  .page-footer {
    position: absolute;
    bottom: 8mm;
    left: 18mm;
    right: 18mm;
    height: 7mm;
    display: flex;
    justify-content: space-between;
    align-items: center;
    font-size: 7.8pt;
    color: #8C7B70;
    border-top: 1px solid #E6D7CD;
  }
  .page-content {
    height: 100%;
    display: flex;
    flex-direction: column;
  }
  
  /* Typography */
  h1.ch-title {
    font-size: 15pt;
    font-weight: bold;
    color: #815621;
    margin: 0 0 4px 0;
    padding-bottom: 3px;
    border-bottom: 2px solid #FEDDBD;
    text-transform: uppercase;
    letter-spacing: 0.5px;
  }
  h2.sec-title {
    font-size: 11pt;
    font-weight: bold;
    color: #2D241E;
    margin: 7px 0 3px 0;
    display: flex;
    align-items: center;
  }
  h2.sec-title::before {
    content: "";
    display: inline-block;
    width: 4px;
    height: 12px;
    background-color: #815621;
    margin-right: 6px;
    border-radius: 1px;
  }
  h3.subsec-title {
    font-size: 9.5pt;
    font-weight: bold;
    color: #5A4334;
    margin: 5px 0 2px 0;
  }
  p {
    margin: 0 0 5px 0;
    text-align: justify;
  }
  ul, ol {
    margin: 0 0 6px 0;
    padding-left: 18px;
  }
  li {
    margin-bottom: 2.5px;
  }
  
  /* Tables */
  table.report-table {
    width: 100%;
    border-collapse: collapse;
    margin: 5px 0 7px 0;
    font-size: 7.8pt;
  }
  table.report-table th {
    background-color: #FEDDBD;
    color: #2D241E;
    font-weight: bold;
    border: 1px solid #D6C2B4;
    padding: 3.5px 6px;
    text-align: left;
  }
  table.report-table td {
    border: 1px solid #E6D7CD;
    padding: 3px 6px;
    vertical-align: top;
  }
  table.report-table tr:nth-child(even) td {
    background-color: #FFF9F5;
  }
  
  /* Boxes and Callouts */
  .callout-box {
    border-radius: 4px;
    padding: 6px 10px;
    margin: 5px 0 7px 0;
    font-size: 8.2pt;
    border-left: 3.5px solid #815621;
    background-color: #FFF5EE;
  }
  .security-box {
    border-radius: 4px;
    padding: 6px 10px;
    margin: 5px 0 7px 0;
    font-size: 8.2pt;
    border-left: 3.5px solid #2E7D32;
    background-color: #F1F8F1;
  }
  .alert-box {
    border-radius: 4px;
    padding: 6px 10px;
    margin: 5px 0 7px 0;
    font-size: 8.2pt;
    border-left: 3.5px solid #C62828;
    background-color: #FFEBEE;
  }
  .box-title {
    font-weight: bold;
    margin-bottom: 2px;
  }
  
  /* Code Excerpts */
  pre.code-block {
    background-color: #2B2119;
    color: #F5EDE6;
    padding: 6px 9px;
    border-radius: 4px;
    font-family: 'Consolas', 'Courier New', monospace;
    font-size: 7.4pt;
    line-height: 1.35;
    margin: 4px 0 6px 0;
    overflow: hidden;
    border: 1px solid #4A3A2F;
  }
  code.inline {
    font-family: 'Consolas', 'Courier New', monospace;
    background-color: #F4EBE3;
    color: #815621;
    padding: 1px 3px;
    border-radius: 2px;
    font-size: 8pt;
  }
  
  /* Figures */
  .figure-container {
    margin: 4px 0 6px 0;
    text-align: center;
    display: flex;
    flex-direction: column;
    align-items: center;
  }
  .figure-img {
    max-width: 100%;
    max-height: 125mm;
    object-fit: contain;
    border-radius: 4px;
    border: 1px solid #E6D7CD;
    background-color: #FFFFFF;
  }
  .screenshot-row {
    display: flex;
    gap: 8px;
    justify-content: center;
    margin: 4px 0;
  }
  .screenshot-col {
    flex: 1;
    display: flex;
    flex-direction: column;
    align-items: center;
  }
  .screenshot-col img {
    max-height: 110mm;
    max-width: 100%;
    border-radius: 6px;
    border: 1px solid #D6C2B4;
    box-shadow: 0 1px 4px rgba(0,0,0,0.06);
  }
  .fig-caption {
    font-size: 7.8pt;
    font-weight: bold;
    color: #815621;
    margin-top: 3px;
    text-align: center;
  }
  .fig-desc {
    font-size: 7.4pt;
    color: #705D53;
    text-align: center;
    margin-top: 1px;
    max-width: 90%;
  }
  
  /* Verification Badge */
  .badge {
    display: inline-block;
    padding: 1.5px 5px;
    border-radius: 3px;
    font-size: 7pt;
    font-weight: bold;
    text-transform: uppercase;
  }
  .badge-source { background-color: #E3F2FD; color: #1565C0; border: 1px solid #BBDEFB; }
  .badge-build { background-color: #E8F5E9; color: #2E7D32; border: 1px solid #C8E6C9; }
  .badge-runtime { background-color: #FFF3E0; color: #E65100; border: 1px solid #FFE0B2; }
  .badge-unverified { background-color: #FFEBEE; color: #C62828; border: 1px solid #FFCDD2; }
  .badge-future { background-color: #F3E5F5; color: #6A1B9A; border: 1px solid #E1BEE7; }
'''

def make_page(page_num, total_pages, header_text, content_html, is_prelim=False):
    footer_page_text = f"Page {page_num} of {total_pages}" if not is_prelim else f"Page {page_num}"
    return f'''
  <div class="page" id="page-{page_num}">
    <div class="page-header">
      <span>{header_text}</span>
      <span>CIPHERVAULT SPECIFICATION</span>
    </div>
    <div class="page-content">
      {content_html}
    </div>
    <div class="page-footer">
      <span>DEPARTMENT OF COMPUTER APPLICATIONS &bull; MCA PROJECT REPORT</span>
      <span>{footer_page_text}</span>
    </div>
  </div>
'''
