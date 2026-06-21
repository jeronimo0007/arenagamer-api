<?php
/**
 * Plugin Name: ArenaGamer Cliente
 * Plugin URI:  https://arenagamer.com
 * Description: Frontend para clientes ArenaGamer — login, torneios, inscrições, créditos e partidas via API.
 * Version:     1.0.2
 * Author:      ArenaGamer
 * Text Domain: arenagamer-cliente
 * Requires at least: 6.0
 * Requires PHP: 7.4
 */

if (!defined('ABSPATH')) {
    exit;
}

define('AG_CLIENTE_VERSION', '1.0.2');
define('AG_CLIENTE_PATH', plugin_dir_path(__FILE__));
define('AG_CLIENTE_URL', plugin_dir_url(__FILE__));

require_once AG_CLIENTE_PATH . 'includes/class-ag-settings.php';
require_once AG_CLIENTE_PATH . 'includes/class-ag-api-proxy.php';
require_once AG_CLIENTE_PATH . 'includes/class-ag-assets.php';
require_once AG_CLIENTE_PATH . 'includes/class-ag-shortcodes.php';

final class ArenaGamer_Cliente_Plugin {

    public static function init(): void {
        AG_Settings::init();
        AG_Api_Proxy::init();
        AG_Assets::init();
        AG_Shortcodes::init();
    }
}

ArenaGamer_Cliente_Plugin::init();
