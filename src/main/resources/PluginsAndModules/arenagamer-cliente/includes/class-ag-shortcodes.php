<?php

if (!defined('ABSPATH')) {
    exit;
}

class AG_Shortcodes {

    /** @var array<string, callable> */
    private static $tags = [];

    public static function init(): void {
        self::register_tag('pagina login', [self::class, 'login']);
        self::register_tag('arenagamer_login', [self::class, 'login']);

        self::register_tag('pagina cadastro', [self::class, 'register']);
        self::register_tag('arenagamer_cadastro', [self::class, 'register']);
        self::register_tag('arenagamer_register', [self::class, 'register']);

        self::register_tag('pagina menu', [self::class, 'menu']);
        self::register_tag('arenagamer_menu', [self::class, 'menu']);

        self::register_tag('pagina torneios', [self::class, 'tournaments']);
        self::register_tag('arenagamer_torneios', [self::class, 'tournaments']);

        self::register_tag('pagina meus torneios', [self::class, 'my_tournaments']);
        self::register_tag('arenagamer_meus_torneios', [self::class, 'my_tournaments']);

        self::register_tag('pagina torneio', [self::class, 'tournament_detail']);
        self::register_tag('arenagamer_torneio', [self::class, 'tournament_detail']);

        self::register_tag('pagina creditos', [self::class, 'credits']);
        self::register_tag('arenagamer_creditos', [self::class, 'credits']);

        self::register_tag('pagina comprar creditos', [self::class, 'buy_credits']);
        self::register_tag('arenagamer_comprar_creditos', [self::class, 'buy_credits']);

        self::register_tag('pagina partidas', [self::class, 'matches']);
        self::register_tag('arenagamer_partidas', [self::class, 'matches']);
    }

    private static function register_tag($tag, $callback): void {
        self::$tags[$tag] = $callback;
        add_shortcode($tag, $callback);
    }

    /** @return string[] */
    public static function get_tags(): array {
        return array_keys(self::$tags);
    }

    private static function render(string $page): string {
        AG_Assets::ensure_loaded();

        ob_start();
        $template = AG_CLIENTE_PATH . 'templates/' . $page . '.php';
        if (file_exists($template)) {
            include $template;
        }
        return ob_get_clean();
    }

    public static function login(): string {
        return self::render('login');
    }

    public static function register(): string {
        return self::render('register');
    }

    public static function menu(): string {
        return self::render('menu');
    }

    public static function tournaments(): string {
        return self::render('tournaments');
    }

    public static function my_tournaments(): string {
        return self::render('my-tournaments');
    }

    public static function tournament_detail($atts): string {
        AG_Assets::ensure_loaded();

        $atts = shortcode_atts(['slug' => ''], $atts, 'pagina torneio');
        ob_start();
        $slug = sanitize_title($atts['slug']);
        include AG_CLIENTE_PATH . 'templates/tournament-detail.php';
        return ob_get_clean();
    }

    public static function credits(): string {
        return self::render('credits');
    }

    public static function buy_credits(): string {
        return self::render('buy-credits');
    }

    public static function matches(): string {
        return self::render('matches');
    }
}
