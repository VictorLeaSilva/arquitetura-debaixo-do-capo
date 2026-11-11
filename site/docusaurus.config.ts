import type { Config } from "@docusaurus/types";
import type * as Preset from "@docusaurus/preset-classic";

const config: Config = {
  title: "Arquitetura Debaixo do Capô",
  tagline:
    "Conceitos de arquitetura num e-commerce com flash sale, em versões ruim, média e boa, medidas lado a lado.",
  url: "https://victorleasilva.github.io",
  // Mesmo endereço da publicação futura no GitHub Pages (Q-032): os links locais e os publicados ficam iguais.
  baseUrl: "/arquitetura-debaixo-do-capo/",

  // Um link interno quebrado derruba o build (AC-010.1).
  onBrokenLinks: "throw",
  markdown: {
    mermaid: true,
    hooks: {
      onBrokenMarkdownLinks: "throw",
    },
  },
  themes: ["@docusaurus/theme-mermaid"],
  plugins: [
    // Q-034: o tema Mermaid importa o pacote opcional @mermaid-js/layout-elk (layouts ELK, que o projeto não usa),
    // e o webpack tenta resolvê-lo mesmo sem ele instalado. O alias "false" o troca por um módulo vazio.
    () => ({
      name: "ignore-mermaid-elk",
      configureWebpack: () => ({
        resolve: { alias: { "@mermaid-js/layout-elk": false } },
      }),
    }),
  ],

  i18n: {
    defaultLocale: "pt-BR",
    locales: ["pt-BR"],
  },

  presets: [
    [
      "classic",
      {
        docs: {
          routeBasePath: "/",
          sidebarPath: "./sidebars.ts",
        },
        blog: false,
      } satisfies Preset.Options,
    ],
  ],

  themeConfig: {
    navbar: {
      title: "Arquitetura Debaixo do Capô",
      items: [
        {
          href: "https://github.com/VictorLeaSilva/arquitetura-debaixo-do-capo",
          label: "GitHub",
          position: "right",
        },
      ],
    },
    footer: {
      copyright: "Código sob MIT; documentação sob CC BY-NC-SA 4.0.",
    },
    prism: {
      additionalLanguages: ["powershell"],
    },
  } satisfies Preset.ThemeConfig,
};

export default config;
