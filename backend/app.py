from flask import Flask, request, jsonify
from flask_cors import CORS
import pandas as pd
from pathlib import Path

app = Flask(__name__)
CORS(app)

# locate dataset (try provided path then fallback)
BASE = Path(__file__).resolve().parent
CSV_PATH = BASE.parent / 'Generator' / 'src' / 'amazon_sales_dataset.csv'

# Lazy load
_df = None

def load_df():
    global _df
    if _df is None:
        if CSV_PATH.exists():
            _df = pd.read_csv(CSV_PATH, parse_dates=['Date'], infer_datetime_format=True)
        else:
            raise FileNotFoundError(f"Dataset not found at {CSV_PATH}")
        # Normalize column names
        _df.columns = [c.strip() for c in _df.columns]
    return _df

@app.route('/stores')
def stores():
    df = load_df()
    stores = sorted(df['Store'].unique().tolist())
    return jsonify(stores)

@app.route('/sales')
def sales():
    df = load_df()
    store = request.args.get('store')
    view = request.args.get('view', 'time_series')
    start = request.args.get('start')
    end = request.args.get('end')
    window = int(request.args.get('window', '7'))

    q = df
    if store:
        # store may be numeric
        try:
            store_val = int(store)
            q = q[q['Store'] == store_val]
        except:
            q = q[q['Store'].astype(str) == store]

    if start:
        q = q[q['Date'] >= start]
    if end:
        q = q[q['Date'] <= end]

    if view == 'time_series':
        # aggregate weekly sales per date
        out = q.groupby('Date')['Weekly_Sales'].sum().reset_index()
        out['Date'] = out['Date'].dt.strftime('%Y-%m-%d')
        return out.to_json(orient='records')
    elif view == 'monthly':
        out = q.assign(Year=q['Date'].dt.year, Month=q['Date'].dt.month)
        out = out.groupby(['Year','Month'])['Weekly_Sales'].sum().reset_index()
        out['ym'] = out['Year'].astype(str) + '-' + out['Month'].astype(str).str.zfill(2)
        return out[['ym','Weekly_Sales']].to_json(orient='records')
    elif view == 'by_dept':
        out = q.groupby('Dept')['Weekly_Sales'].sum().reset_index()
        return out.to_json(orient='records')
    elif view == 'moving_avg':
        ts = q.groupby('Date')['Weekly_Sales'].sum().sort_index()
        ma = ts.rolling(window=window, min_periods=1).mean().reset_index()
        ma['Date'] = ma['Date'].dt.strftime('%Y-%m-%d')
        ma.columns = ['Date','value']
        return ma.to_json(orient='records')
    elif view == 'mom_pct':
        out = q.assign(ym=q['Date'].dt.to_period('M'))
        m = out.groupby('ym')['Weekly_Sales'].sum().reset_index()
        m['pct'] = m['Weekly_Sales'].pct_change().fillna(0)*100
        m['ym'] = m['ym'].astype(str)
        return m[['ym','pct']].to_json(orient='records')
    else:
        return jsonify({'error':'unknown view'}), 400

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000, debug=True)
