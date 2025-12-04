export interface Threshold {
  name: string;
  value: number;
}

export interface Kpi {
  displayName: string;
  score: number;
  resultType?: string;
  id: string;
  children: Kpi[];
  thresholds?: Threshold[];
  description?: string;
  tags?: string[];
}

export interface RepoInfo {
  stars?: number;
  lastCommitDate?: string;
  contributors?: number;
  projectUrl?: string;
  projectName?: string;
  version?: string;
  repoLanguages: Language[];
}

export interface Language {
  name: string;
  size: number;
}

export interface ToolInfo {
  name: string;
  description: string;
  version?: string;
}

export interface ToolInfoAndOrigin {
  toolInfo: ToolInfo;
  origins: any[];
}

export interface Tool {
  name: string;
  scanDate?: string;
  findings?: any[];
  description?: string;
}

export interface Result {
  healthScore: number;
  repoInfo: RepoInfo;
  root: Kpi;
  tools: Tool[];
  createdAt: string; // defaults to upload date
}

export class Product {
  id: string;
  name: string;
  description?: string;
  version?: string;
  results: Result[];
  createdAt?: string;
  // TODO: add product group

  constructor(
    id: string,
    name: string,
    results: Result[] = [],
    description?: string,
    version?: string,
    createdAt?: string
  ) {
    this.id = id;
    this.name = name;
    this.results = results;
    this.description = description;
    this.version = version;
    this.createdAt = createdAt;
  }

  /**
   * Get the newest version of the product from its latest result
   */
  getNewestVersion(): string {
    if (this.results.length === 0) return 'Unknown';
    const lastResult = this.results[this.results.length - 1];
    return lastResult?.repoInfo.version || 'Unknown';
  }

  /**
   * Get the list of programming languages used in the product
   */
  getUsedLanguages(): string {
    if (this.results.length === 0) return 'Unknown';
    const lastResult = this.results[this.results.length - 1];
    return lastResult?.repoInfo.repoLanguages.map(lang => lang.name).join(', ') || 'Unknown';
  }

  /**
   * Get the project URL from the product's latest result
   */
  getProjectUrl(): string {
    if (this.results.length === 0) return '#';
    const lastResult = this.results[this.results.length - 1];
    return lastResult?.repoInfo.projectUrl || '#';
  }

  /**
   * Get the current health score from the product's latest result
   */
  getCurrentHealthScore(): number | null {
    if (this.results.length === 0) return null;
    const lastResult = this.results[this.results.length - 1];
    return lastResult?.healthScore ?? null;
  }

  /**
   * Calculate the health score trend between the last two results
   * Returns the difference (positive for improvement, negative for decline)
   */
  getHealthScoreTrend(): number | null {
    if (this.results.length < 2) return null;
    const currentScore = this.results[this.results.length - 1]?.healthScore;
    const previousScore = this.results[this.results.length - 2]?.healthScore;

    if (currentScore === undefined || previousScore === undefined) return null;
    
    return currentScore - previousScore;
  }

  /**
   * Extract health score data for charting purposes
   * Returns labels (dates) and data (health scores) arrays
   */
  getHealthDataForChart(): { labels: string[], data: number[] } {
    if (this.results.length === 0) {
      return { labels: [], data: [] };
    }
    
    const labels: string[] = [];
    const data: number[] = [];
    
    this.results.forEach((result) => {
      const date = new Date(result.createdAt);
      labels.push(date.toLocaleDateString());
      data.push(result.healthScore);
    });
    
    return { labels, data };
  }

  /**
   * Get the CSS color class for the current health score
   */
  getHealthScoreColorClass(): string {
    const score = this.getCurrentHealthScore();
    if (score === null) return 'bg-light text-muted';
    if (score >= 70) return 'bg-success bg-gradient';
    if (score >= 50) return 'bg-warning bg-gradient text-dark';
    return 'bg-danger bg-gradient';
  }

  /**
   * Get the CSS color class for the current trend value
   */
  getTrendColorClass(): string {
    const trend = this.getHealthScoreTrend();
    if (trend === null) return 'text-muted';
    if (trend > 0) return 'text-success';
    if (trend < 0) return 'text-danger';
    return 'text-muted'; // for trend === 0
  }

  /**
   * Get the Bootstrap icon class for the current trend value
   */
  getTrendIcon(): string {
    const trend = this.getHealthScoreTrend();
    if (trend === null) return '';
    if (trend > 0) return 'bi-arrow-up';
    if (trend < 0) return 'bi-arrow-down';
    return 'bi-dash'; // for trend === 0
  }
}
