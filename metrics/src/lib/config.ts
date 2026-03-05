import { getProposeMetricUrl } from "./proposeMetric";

type LinkConfig = {
  id: "github" | "linkedin" | "fraunhofer";
  label: string;
  url: string;
  ariaLabel: string;
  title: string;
  iconPath: string;
};

const github: LinkConfig = {
  id: "github",
  label: "GitHub",
  url: "https://github.com/fraunhofer-iem/spha",
  ariaLabel: "SPHA on GitHub",
  title: "GitHub",
  iconPath:
    "M12 2.25c-5.385 0-9.75 4.365-9.75 9.75 0 4.311 2.787 7.964 6.657 9.254.486.09.663-.205.663-.462 0-.23-.009-.999-.013-1.812-2.706.588-3.277-1.156-3.277-1.156-.442-1.124-1.08-1.423-1.08-1.423-.883-.603.067-.592.067-.592 1.0.07 1.528 1.026 1.528 1.026.888 1.521 2.332 1.083 2.9.827.09-.643.348-1.083.633-1.332-2.16-.246-4.432-1.08-4.432-4.806 0-1.061.378-1.928 1.0-2.606-.1-.246-.433-1.237.095-2.578 0 0 .816-.261 2.67.996a9.23 9.23 0 0 1 2.43-.327c.825.004 1.656.111 2.43.327 1.852-1.257 2.667-.996 2.667-.996.53 1.341.197 2.332.097 2.578.623.678 1.0 1.545 1.0 2.606 0 3.735-2.276 4.556-4.442 4.797.357.307.675.912.675 1.839 0 1.328-.013 2.397-.013 2.723 0 .26.174.556.67.462 3.867-1.292 6.65-4.943 6.65-9.253 0-5.385-4.365-9.75-9.75-9.75Z",
};

const linkedin: LinkConfig = {
  id: "linkedin",
  label: "LinkedIn",
  url: "https://www.linkedin.com/company/software-product-health-assistant",
  ariaLabel: "SPHA on LinkedIn",
  title: "LinkedIn",
  iconPath:
    "M5.5 8.5h-3v10h3v-10Zm.25-3.25a1.75 1.75 0 1 1-3.5 0 1.75 1.75 0 0 1 3.5 0ZM20.5 12.75v5.75h-3v-5.35c0-1.335-.475-2.245-1.665-2.245-.91 0-1.45.61-1.69 1.198-.088.213-.11.51-.11.807v5.59h-3s.04-9.07 0-10h3v1.416c.398-.615 1.11-1.49 2.705-1.49 1.975 0 3.46 1.29 3.46 4.074Z",
};

const fraunhofer: LinkConfig = {
  id: "fraunhofer",
  label: "Fraunhofer IEM",
  url: "https://www.iem.fraunhofer.de",
  ariaLabel: "Fraunhofer IEM",
  title: "Fraunhofer IEM",
  iconPath:
    "M12 2.5a9.5 9.5 0 1 0 0 19 9.5 9.5 0 0 0 0-19Zm6.98 9.25h-3.03c-.1-2.165-.567-4.19-1.35-5.62a7.53 7.53 0 0 1 4.38 5.62Zm-6.98 7.0c-.9 0-2.16-2.09-2.38-5.0h4.76c-.22 2.91-1.48 5.0-2.38 5.0Zm-2.38-7.0c.22-2.91 1.48-5.0 2.38-5.0s2.16 2.09 2.38 5.0h-4.76Zm-.25-5.62c-.783 1.43-1.25 3.455-1.35 5.62H5.02a7.53 7.53 0 0 1 4.35-5.62Zm-4.35 7.62h3.03c.1 2.165.567 4.19 1.35 5.62a7.53 7.53 0 0 1-4.38-5.62Zm9.63 5.62c.783-1.43 1.25-3.455 1.35-5.62h3.03a7.53 7.53 0 0 1-4.38 5.62Z",
};

export const proposeMetricUrl = getProposeMetricUrl();
export const headerSocialLinks = [github, linkedin];
export const footerLinks = [github, linkedin, fraunhofer];
export const getRepoUrl = () => import.meta.env.VITE_REPO_URL;
export const getRepoBranch = () => import.meta.env.VITE_REPO_BRANCH ?? "main";
export type { LinkConfig };
