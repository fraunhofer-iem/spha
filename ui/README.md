# SPHA Visualization

A web-based dashboard for visualizing Software Project Health Assistant (SPHA) results. This Vue.js application provides comprehensive insights into software project health metrics, security vulnerabilities, and quality indicators.

## Features

### 📊 Interactive Dashboard
- **Health Score Visualization** - Overall project health assessment with visual indicators
- **KPI Hierarchy Display** - Hierarchical view of Key Performance Indicators
- **Project Overview** - Repository information, contributors, and language breakdown

### 🔧 Tool Integration
Support for multiple analysis tools including:
- **TruffleHog** - Secret detection
- **OSV** - Open Source Vulnerabilities database
- **GitHub** - Repository analysis and security features
- **Technical Lag** - SBOM-based calculation of dependency lag

### 📈 Data Visualization
- Interactive charts and graphs using Chart.js
- Language distribution pie charts
- KPI trend analysis
- Threshold-based warning systems

## Getting Started

### Prerequisites
- Node.js (v18 or higher)
- npm or yarn package manager

### Installation

1. Clone the repository:
```bash
git clone https://github.com/fraunhofer-iem/spha-ui
cd spha-ui
```

2. Install dependencies:
```bash
npm install
```

3. Start the development server:
```bash
npm run dev
```

4. Open your browser and navigate to `http://localhost:5173`

### Usage

1. **Load Analysis Results**: Drop a JSON file containing SPHA analysis results onto the file upload area
2. **Explore Dashboard**: Navigate through different sections to examine project health metrics
3. **Analyze KPIs**: Drill down into specific Key Performance Indicators for detailed insights
4. **Review Security Findings**: Examine vulnerability reports and security recommendations

## Development

### Available Scripts

**Development:**
- `npm run dev` - Start development server
- `npm run dev:demo` - Start development server in demo mode
- `npm run build` - Build for production
- `npm run build:demo` - Build for production in demo mode
- `npm run preview` - Preview production build

**Testing:**
- `npm run test` - Run tests in watch mode
- `npm run test:unit` - Run unit tests (util & model)
- `npm run test:components` - Run component tests
- `npm run test:integration` - Run integration tests
- `npm run test:all` - Run all test suites sequentially
- `npm run test:coverage` - Generate test coverage report
- `npm run test:coverage:unit` - Generate coverage for unit tests
- `npm run test:coverage:components` - Generate coverage for component tests
- `npm run test:ui` - Run tests with UI interface
- `npm run test:ci` - Run tests for CI with JUnit and JSON reports
- `npm run test:run` - Run tests once (no watch)
- `npm run test:watch` - Run tests in watch mode
- `npm run test:debug` - Run tests with debugger
- `npm run test:performance` - Run tests with heap usage logging
- `npm run test:verbose` - Run tests with verbose output

### Project Structure

```
src/
├── components/        # Reusable Vue components
├── views/            # Page-level components
├── composables/      # Vue composition functions
├── model/            # TypeScript interfaces and models
├── util/             # Utility functions and helpers
├── assets/           # Static assets (images, styles)
│   ├── img/          # Images including supported tools
│   └── styles/       # SASS/CSS styles
├── __tests__/        # Integration test files
├── __test__/         # Additional test directory
└── *//__tests__/     # Component and utility test files
```

### Testing

The project includes comprehensive testing setup:

- **Unit Tests** - Testing individual functions and utilities
- **Component Tests** - Testing Vue component behavior
- **Integration Tests** - Testing component interactions
- **Coverage Reports** - Detailed test coverage analysis

Run specific test suites:
```bash
npm run test:unit          # Unit tests only
npm run test:components    # Component tests only
npm run test:integration   # Integration tests only
npm run test:coverage      # With coverage report
```

## Data Format

The application expects JSON files with the following structure:

```json
{
  "healthScore": 75,
  "repoInfo": {
    "projectName": "Example Project",
    "contributors": 5,
    "stars": 100,
    "lastCommitDate": "2024-01-15",
    "repoLanguages": [
      {
        "name": "TypeScript",
        "size": 65.2
      }
    ]
  },
  "root": {
    "displayName": "Project Health",
    "score": 75,
    "id": "root",
    "children": [...]
  },
  "tools": [
    {
      "name": "TruffleHog",
      "scanDate": "2024-01-15",
      "findings": 0,
      "downloadLink": "...",
      "description": "Secret detection tool"
    }
  ]
}
```

### Example Data

Sample data files are available in the `example/` directory:
- `kpi-results.json` - Full example with comprehensive metrics
- `kpi-results-small.json` - Simplified example for testing

## Technologies Used

- **Vue 3** - Progressive JavaScript framework
- **TypeScript** - Type-safe JavaScript development
- **Vite** - Fast build tool and development server
- **Bootstrap 5** - CSS framework for responsive design
- **Chart.js** - Interactive charts and visualizations
- **Vitest** - Unit testing framework
- **SASS** - CSS preprocessor

## Browser Support

- Chrome (latest)
- Firefox (latest)
- Safari (latest)
- Edge (latest)

## Contributing

1. Fork the repository
2. Create a feature branch: `git checkout -b feature/new-feature`
3. Commit your changes: `git commit -am 'Add new feature'`
4. Push to the branch: `git push origin feature/new-feature`
5. Submit a pull request

### Development Guidelines

- Follow TypeScript best practices
- Write comprehensive tests for new features
- Maintain consistent code style
- Update documentation for API changes

## License

This project is part of the SPHA (Software Project Health Analysis) suite developed by Fraunhofer IEM.

---

For questions or support, please refer to the project documentation or open an issue in the repository.
