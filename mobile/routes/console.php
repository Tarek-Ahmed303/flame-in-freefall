<?php

use Illuminate\Foundation\Inspiring;
use Illuminate\Support\Facades\Artisan;
use App\Services\PushNotificationService;

Artisan::command('inspire', function () {
    $this->comment(Inspiring::quote());
})->purpose('Display an inspiring quote');


Artisan::command('firebase:test', function (PushNotificationService $service) {
    $service->sendToUser(
        'test_fcm_token_123',
        'Firebase test body',
        'PyroNova Test',
        ['action' => 'open_chatbot']
    );

    $this->info('Notification sent successfully.');
});
