import './TickerTape.css'

const TICKERS = [
  { symbol: 'S&P 500',    value: '5,847.23', change: '+0.42%',  up: true  },
  { symbol: 'DJIA',       value: '43,216.45', change: '+0.18%', up: true  },
  { symbol: 'NASDAQ',     value: '18,563.10', change: '+0.71%', up: true  },
  { symbol: 'VFIAX',      value: '$521.43',  change: '+1.07%',  up: true  },
  { symbol: 'FDGRX',      value: '$89.72',   change: '+1.38%',  up: true  },
  { symbol: 'TRBCX',      value: '$152.91',  change: '+0.92%',  up: true  },
  { symbol: 'SWTSX',      value: '$78.34',   change: '+0.61%',  up: true  },
  { symbol: 'PTTRX',      value: '$8.83',    change: '-0.12%',  up: false },
  { symbol: '10Y UST',    value: '4.23%',    change: '-0.05',   up: false },
  { symbol: 'VIX',        value: '14.83',    change: '-2.10%',  up: false },
  { symbol: 'GOLD',       value: '$2,341',   change: '+0.31%',  up: true  },
  { symbol: 'USD/EUR',    value: '0.9241',   change: '-0.08%',  up: false },
  { symbol: 'CRUDE OIL',  value: '$78.64',   change: '+0.55%',  up: true  },
]

// Duplicate the array so the marquee loops seamlessly
const ITEMS = [...TICKERS, ...TICKERS]

export default function TickerTape() {
  return (
    <div className="ticker">
      <div className="ticker__label">LIVE</div>
      <div className="ticker__track">
        <div className="ticker__inner">
          {ITEMS.map((item, i) => (
            <span key={i} className="ticker__item">
              <span className="ticker__symbol">{item.symbol}</span>
              <span className="ticker__value">{item.value}</span>
              <span className={`ticker__change ticker__change--${item.up ? 'up' : 'down'}`}>
                {item.up ? '▲' : '▼'} {item.change}
              </span>
              <span className="ticker__sep">·</span>
            </span>
          ))}
        </div>
      </div>
    </div>
  )
}
