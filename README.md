# 🚀 AI-Powered Tech News Summarizer

An intelligent news aggregation system that fetches the latest tech news from Techmeme, processes them with AI, and presents them in a beautiful web interface with real-time refresh capabilities.

## ✨ Features

- 🔄 **Real-time Data Fetching** from Techmeme RSS feeds
- 🤖 **AI-Powered Summarization** using Google Gemini 2.0 Flash
- 🌐 **Modern Web Interface** with responsive design
- ⚡ **Live Refresh Button** with visual feedback and progress tracking
- 📱 **Mobile-Friendly** with floating refresh button
- 🔍 **Search & Filter** functionality for news items
- 📊 **Analytics Dashboard** with news statistics
- 🎨 **Beautiful Animations** and loading effects

## 🏗️ Architecture

```
e:\Viscode\Demo_Skola/
├── 📄 README.md                    # Project documentation
├── 🔑 .env                         # Environment variables (API keys)
├── 🌐 news.html                    # Main web interface
├── 🎨 styles.css                   # Responsive CSS with animations
├── ⚡ js/main.js                    # Frontend JavaScript logic
├── 📊 summaries.json               # Generated news data
├── 🖥️ serve_website.py             # Web server with API endpoints
├── 🔄 update_news.py               # Full AI update pipeline
├── ⚡ simple_update.py             # Quick update without AI
├── 🧪 quick_refresh.py             # Test refresh functionality
│
├── 📁 src/                         # Core Python modules
│   ├── 🔌 feeds/
│   │   ├── techmeme.py             # RSS fetching and parsing
│   │   └── techmeme/
│   │       └── client.py           # Techmeme API client
│   ├── 🔧 pipelines/
│   │   ├── main_pipeline.py        # Main processing pipeline
│   │   └── news_pipeline.py        # News-specific pipeline
│   ├── 🤖 services/
│   │   ├── summarize_with_gemini.py # AI summarization service
│   │   ├── feed_service.py         # Feed processing service
│   │   └── storage_service.py      # Data storage service
│   ├── ⚙️ config/
│   │   └── settings.py             # Configuration management
│   ├── 📋 domain/
│   │   └── models.py               # Data models
│   └── 🛠️ utils/
│       ├── json_tools.py           # JSON utilities
│       └── batching.py             # Batch processing
│
├── 📁 data/                        # Data storage
│   ├── raw/                        # Raw RSS data
│   │   └── techmeme_sample_full.json
│   └── outputs/                    # Processed data
│       └── summaries.json
│
├── 📁 public/                      # Static web assets
│   └── index.html                  # Alternative homepage
│
└── 📁 docs/                        # Documentation
    ├── RUN_GUIDE.md                # How to run the system
    ├── REAL_REFRESH_GUIDE.md       # Refresh functionality guide
    ├── REFRESH_FIX_REPORT.md       # Bug fix documentation
    └── WEBSITE_README.md           # Web interface documentation
```

## 🛠️ Prerequisites

### Required Software

- **Python 3.10+** (tested with Python 3.13)
- **Internet Connection** for fetching news and AI processing

### Required Python Packages

```bash
pip install requests feedparser beautifulsoup4 google-generativeai python-dotenv
```

### API Requirements

- **Google Gemini API Key** (free tier available)
- Get your key from: https://makersuite.google.com/app/apikey

## 🚀 Quick Start

### 1. Clone and Setup

```powershell
cd e:\Viscode\Demo_Skola
```

### 2. Configure Environment

Create `.env` file:

```env
GEMINI_API_KEY=your_actual_api_key_here
```

### 3. Install Dependencies

```powershell
pip install requests feedparser beautifulsoup4 google-generativeai python-dotenv
```

### 4. Run the Application

```powershell
# Start the web server
py -3.13 serve_website.py

# Open browser to: http://localhost:8000
```

## 📖 Usage Guide

### 🌐 Web Interface

1. **Open Website**: Navigate to `http://localhost:8000`
2. **Browse News**: Scroll through AI-summarized tech news
3. **Search**: Use search bar to find specific topics
4. **Filter**: Filter by news type (news, announcements, etc.)
5. **Refresh**: Click floating refresh button (top-left) for new data

### 🔄 Data Updates

#### Automatic Refresh (Recommended)

- Click the **floating refresh button** in top-left corner
- Watch real-time progress messages
- System fetches fresh data from Techmeme
- AI processes and summarizes content
- UI updates automatically

#### Manual Update

```powershell
# Full AI-powered update
py -3.13 update_news.py

# Quick update without AI
py -3.13 simple_update.py

# Test refresh functionality
py -3.13 quick_refresh.py
```

### 🧪 Testing & Development

```powershell
# Test Techmeme data fetching
py -3.13 test_techmeme.py

# Test integration
py -3.13 test_integration.py

# Quick functionality test
py -3.13 test_quick.py
```

## 🎨 Web Interface Features

### 🔄 Floating Refresh Button

- **Location**: Top-left corner
- **States**:
  - 🔵 Normal: Blue gradient with pulse effect
  - 🟡 Loading: Gray with spinning icon
  - 🟢 Success: Green with checkmark
  - 🔴 Error: Red with warning icon
- **Progress**: Real-time status messages during refresh

### 📱 Responsive Design

- **Desktop**: Full-width layout with sidebar stats
- **Tablet**: Adapted columns and navigation
- **Mobile**: Stack layout with optimized touch targets

### 🎯 Interactive Elements

- **Search**: Real-time filtering as you type
- **Hover Effects**: Smooth transitions and highlights
- **Loading Overlay**: Backdrop blur with progress indicators
- **Animations**: CSS keyframes for smooth UX

## 🤖 AI Integration

### Gemini 2.0 Flash Model

- **Purpose**: Summarize news articles into bullet points
- **Input**: Raw news content from Techmeme
- **Output**: Structured summaries with key insights
- **Features**:
  - Bullet point extraction
  - "Why it matters" analysis
  - Key command identification
  - Caveat highlighting

### Data Processing Pipeline

1. **Fetch**: Get latest news from Techmeme RSS
2. **Parse**: Extract URLs, titles, and content
3. **Enrich**: Fetch additional metadata from source articles
4. **Summarize**: Process with Gemini AI
5. **Format**: Structure into JSON for web display
6. **Store**: Save to `summaries.json`

## 🔧 API Endpoints

### Server API

- `GET /` → Redirects to `/news.html`
- `GET /news.html` → Main web interface
- `GET /summaries.json` → Current news data
- `GET /api/refresh` → Start data refresh process
- `GET /api/refresh/status` → Check refresh progress

### Refresh Process

1. **Start**: `POST /api/refresh`
2. **Monitor**: `GET /api/refresh/status` (polls every 5s)
3. **Complete**: Status shows `completed: true`
4. **Update**: Frontend fetches new `summaries.json`

## 📊 Data Format

### News Item Structure

```json
{
  "title": "OpenAI announces GPT-5 with breakthrough capabilities",
  "url": "https://example.com/article",
  "bullets": [
    "OpenAI releases GPT-5 with 10x improvement over GPT-4",
    "New model shows advanced reasoning and multimodal abilities",
    "Available to ChatGPT Plus subscribers starting next month"
  ],
  "why_it_matters": "This represents a significant leap in AI capabilities...",
  "type": "news",
  "key_commands": ["openai", "gpt-5", "ai"],
  "caveats": [
    "Limited initial availability",
    "Higher computational requirements"
  ]
}
```

### Summary File Structure

```json
{
  "items": [
    /* array of news items */
  ],
  "last_updated": "2025-10-09T12:00:00Z",
  "total_items": 15,
  "metadata": {
    "source": "techmeme",
    "processed_with": "gemini-2.0-flash",
    "version": "1.0"
  }
}
```

## 🐛 Troubleshooting

### Common Issues

#### Python Environment

```powershell
# If python command fails, use py launcher
py -3.13 serve_website.py

# Check Python installation
py --list
```

#### Missing Dependencies

```powershell
# Install missing packages
pip install google-generativeai python-dotenv

# Or install all at once
pip install -r requirements.txt
```

#### API Key Issues

1. Verify `.env` file exists and contains correct key
2. Test API key: `py -3.13 test_integration.py`
3. Check Gemini API quota and billing

#### Port Conflicts

```powershell
# Kill existing Python processes
Get-Process | Where-Object {$_.ProcessName -eq "python"} | Stop-Process -Force

# Or use different port in serve_website.py
```

### Debugging Features

- **Console Logs**: Open browser DevTools → Console
- **Server Logs**: Check terminal output
- **Network Tab**: Monitor API calls in DevTools
- **Error Messages**: Detailed error reporting in UI

## 🔒 Security Notes

- **API Keys**: Never commit `.env` to version control
- **CORS**: Server allows localhost connections only
- **Input Validation**: Server validates all API inputs
- **Rate Limiting**: Gemini API has built-in rate limits

## 🚀 Performance Optimization

### Caching Strategy

- **Browser Cache**: Static assets cached for 1 hour
- **Data Cache**: News data cached until refresh
- **API Cache**: Gemini responses cached per content hash

### Resource Management

- **Memory**: Efficient JSON parsing and storage
- **Network**: Batch API calls where possible
- **CPU**: Background processing for AI tasks

## 📈 Future Enhancements

### Planned Features

- [ ] Multiple news source integration
- [ ] User customization preferences
- [ ] Dark/light theme toggle
- [ ] Email/notification alerts
- [ ] Historical data archive
- [ ] Advanced analytics dashboard

### Technical Improvements

- [ ] Redis caching layer
- [ ] Docker containerization
- [ ] CI/CD pipeline setup
- [ ] Automated testing suite
- [ ] Performance monitoring

## 📄 License

This project is for educational and personal use. Please respect Techmeme's terms of service and API rate limits.

## 🤝 Contributing

1. Fork the repository
2. Create feature branch: `git checkout -b feature-name`
3. Commit changes: `git commit -am 'Add feature'`
4. Push to branch: `git push origin feature-name`
5. Submit pull request

## 📞 Support

For issues and questions:

1. Check existing documentation in `/docs/` folder
2. Review troubleshooting section above
3. Create GitHub issue with detailed description

---

**Last Updated**: October 9, 2025
**Version**: 2.0.0
**Status**: ✅ Production Ready
