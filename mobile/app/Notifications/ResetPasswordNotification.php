<?php

namespace App\Notifications;

use Illuminate\Bus\Queueable;
use Illuminate\Notifications\Messages\MailMessage;
use Illuminate\Notifications\Notification;

class ResetPasswordNotification extends Notification
{
    use Queueable;

    public function __construct(
        public string $token
    ) {}

    public function via(object $notifiable): array
    {
        return ['mail'];
    }

    public function toMail(object $notifiable): MailMessage
    {
       $url = url('/reset-password') . '?' . http_build_query([
    'token' => $this->token,
    'email' => $notifiable->getEmailForPasswordReset(),
]);

        return (new MailMessage)
            ->subject('Reset Your Password - PyroNova')
            ->view('emails.reset-password', [
                'url' => $url,
            ]);
    }
}
