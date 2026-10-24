<?php

namespace App\Services;

use App\Enums\AssignedModule;
use App\Models\User;
use Kreait\Firebase\Contract\Messaging;
use Kreait\Firebase\Messaging\CloudMessage;
use Kreait\Firebase\Messaging\Notification as FcmNotification;

class PushNotificationService
{
    public function __construct(
        private Messaging $messaging
    ) {}

    public function sendToModule(
        AssignedModule $module,
        string $title,
        string $body
    ): void {
      $users = User::query()
    ->where('assigned_module', $module->value)
    ->whereNotNull('fcm_token')
    ->where('fcm_token', '!=', '')
    ->get();

if ($users->isEmpty()) {
    abort(422, 'No registered devices found for this module.');
}

$users->each(function (User $user) use ($title, $body) {
    $message = CloudMessage::new()
        ->toToken($user->fcm_token)
        ->withNotification(
            FcmNotification::create($title, $body)
        )
        ->withData([
            'action' => 'Open AI Steps',
        ]);

    $this->messaging->send($message);
});
    }}
