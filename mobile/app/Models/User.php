<?php

namespace App\Models;
use App\Notifications\ResetPasswordNotification;
 use Illuminate\Contracts\Auth\MustVerifyEmail;
use Database\Factories\UserFactory;
use Illuminate\Database\Eloquent\Attributes\Fillable;
use Illuminate\Database\Eloquent\Attributes\Hidden;
use Illuminate\Database\Eloquent\Factories\HasFactory;
use Illuminate\Foundation\Auth\User as Authenticatable;
use Illuminate\Notifications\Notifiable;
use Laravel\Sanctum\HasApiTokens;

use App\Enums\AssignedModule;
#[Fillable(['name', 'email', 'password','assigned_module','fcm_token'])]
#[Hidden(['password', 'remember_token'])]
class User extends Authenticatable implements MustVerifyEmail
{
    /** @use HasFactory<UserFactory> */
    use HasApiTokens, HasFactory, Notifiable;

    /**
     * Get the attributes that should be cast.
     *
     * @return array<string, string>
     */
    public function sendPasswordResetNotification($token): void
{
    $this->notify(new ResetPasswordNotification($token));
}
    protected function casts(): array
    {
        return [
            'email_verified_at' => 'datetime',
            'password' => 'hashed',
            'assigned_module' =>AssignedModule::class
        ];
    }
    public function notifications(){
        return $this->hasMany(Notifications::class);
    }
     public function messages(){
        return $this->hasMany(Message::class);
    }
}
