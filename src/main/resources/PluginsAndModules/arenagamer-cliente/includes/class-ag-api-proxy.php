<?php

if (!defined('ABSPATH')) {
    exit;
}

class AG_Api_Proxy {

    private const CATALOG_PATHS = [
        '/api/v1/public/plans',
        '/api/v1/public/tournaments',
        '/api/v1/public/presets',
        '/api/v1/public/tournament-pricing',
    ];

    public static function init(): void {
        add_action('wp_ajax_arenagamer_api', [self::class, 'handle']);
        add_action('wp_ajax_nopriv_arenagamer_api', [self::class, 'handle']);
    }

    public static function handle(): void {
        check_ajax_referer('arenagamer_api', 'nonce');

        $settings = AG_Settings::get();
        $api_url = rtrim($settings['api_url'] ?? '', '/');

        if ($api_url === '') {
            wp_send_json_error(['message' => 'URL da API não configurada. Vá em Configurações → ArenaGamer Cliente.'], 500);
        }

        $path = sanitize_text_field(wp_unslash($_POST['path'] ?? ''));
        if ($path === '' || strpos($path, '/api/v1/') !== 0) {
            wp_send_json_error(['message' => 'Caminho da API inválido.'], 400);
        }

        $method = strtoupper(sanitize_text_field(wp_unslash($_POST['method'] ?? 'GET')));
        if (!in_array($method, ['GET', 'POST', 'PUT', 'PATCH', 'DELETE'], true)) {
            wp_send_json_error(['message' => 'Método HTTP inválido.'], 400);
        }

        $url = $api_url . $path;
        $headers = ['Content-Type' => 'application/json', 'Accept' => 'application/json'];

        $token = sanitize_text_field(wp_unslash($_POST['token'] ?? ''));
        if ($token !== '') {
            $headers['Authorization'] = 'Bearer ' . $token;
        } elseif (self::needs_catalog_basic_auth($path, $method)) {
            $email = $settings['catalog_email'] ?? '';
            $pass = $settings['catalog_pass'] ?? '';
            if ($email === '' || $pass === '') {
                wp_send_json_error(['message' => 'Configure e-mail e senha do catálogo em Configurações → ArenaGamer Cliente.'], 500);
            }
            $headers['Authorization'] = 'Basic ' . base64_encode($email . ':' . $pass);
        }

        $args = [
            'method'  => $method,
            'headers' => $headers,
            'timeout' => 30,
        ];

        $raw_body = wp_unslash($_POST['body'] ?? '');
        if ($raw_body !== '' && in_array($method, ['POST', 'PUT', 'PATCH'], true)) {
            $args['body'] = $raw_body;
        }

        $response = wp_remote_request($url, $args);

        if (is_wp_error($response)) {
            wp_send_json_error([
                'message' => 'Não foi possível conectar à API em ' . $api_url . '. Verifique se o servidor está rodando.',
                'detail'  => $response->get_error_message(),
            ], 502);
        }

        $status = wp_remote_retrieve_response_code($response);
        $body = wp_remote_retrieve_body($response);
        $data = json_decode($body, true);

        if (!is_array($data)) {
            wp_send_json_error(['message' => 'Resposta inválida da API.'], 502);
        }

        if ($status >= 400) {
            wp_send_json_error([
                'message' => $data['message'] ?? 'Erro na API',
                'code'    => $data['code'] ?? null,
                'data'    => $data,
            ], $status);
        }

        wp_send_json_success($data);
    }

    private static function needs_catalog_basic_auth(string $path, string $method): bool {
        if ($method !== 'GET') {
            return false;
        }
        $base = strtok($path, '?');
        return in_array($base, self::CATALOG_PATHS, true);
    }
}
