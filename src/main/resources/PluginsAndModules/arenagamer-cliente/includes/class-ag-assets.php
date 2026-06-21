<?php

if (!defined('ABSPATH')) {
    exit;
}

class AG_Assets {

    private static bool $loaded = false;

    public static function init(): void {
        add_action('wp_enqueue_scripts', [self::class, 'maybe_enqueue'], 20);
    }

    public static function ensure_loaded(): void {
        self::enqueue_assets();
    }

    public static function maybe_enqueue(): void {
        if (self::$loaded || !self::has_shortcode_on_page()) {
            return;
        }
        self::enqueue_assets();
    }

    private static function enqueue_assets(): void {
        if (self::$loaded) {
            return;
        }
        self::$loaded = true;

        $settings = AG_Settings::get();
        $packages = array_values(array_filter(array_map(
            static fn($line) => trim($line),
            explode("\n", $settings['credit_packages'])
        )));

        wp_enqueue_style(
            'arenagamer-cliente',
            AG_CLIENTE_URL . 'assets/css/arenagamer-cliente.css',
            [],
            AG_CLIENTE_VERSION
        );

        wp_enqueue_script(
            'arenagamer-cliente-api',
            AG_CLIENTE_URL . 'assets/js/api-client.js',
            [],
            AG_CLIENTE_VERSION,
            true
        );

        wp_enqueue_script(
            'arenagamer-cliente-app',
            AG_CLIENTE_URL . 'assets/js/app.js',
            ['arenagamer-cliente-api'],
            AG_CLIENTE_VERSION,
            true
        );

        wp_localize_script('arenagamer-cliente-api', 'ArenaGamerConfig', [
            'ajaxUrl'        => admin_url('admin-ajax.php'),
            'nonce'          => wp_create_nonce('arenagamer_api'),
            'apiUrl'         => $settings['api_url'],
            'loginUrl'       => $settings['login_url'],
            'creditPackages' => $packages,
            'i18n'           => [
                'loading'       => 'Carregando…',
                'error'         => 'Ocorreu um erro. Tente novamente.',
                'loginRequired' => 'Faça login para continuar.',
                'noData'        => 'Nenhum registro encontrado.',
                'success'       => 'Operação realizada com sucesso!',
                'apiDown'       => 'API indisponível. Verifique se o servidor ArenaGamer está rodando na porta 8080.',
            ],
        ]);
    }

    private static function has_shortcode_on_page(): bool {
        global $post;
        if (!$post instanceof WP_Post) {
            return false;
        }

        $content = $post->post_content;

        foreach (AG_Shortcodes::get_tags() as $tag) {
            if (has_shortcode($content, $tag)) {
                return true;
            }
        }

        if (strpos($content, '[arenagamer_') !== false) {
            return true;
        }

        if (strpos($content, '[pagina ') !== false) {
            return true;
        }

        return false;
    }
}
