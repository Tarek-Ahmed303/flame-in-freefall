
<?php

namespace App\Http\Controllers\Auth;

use App\Http\Controllers\Controller;
use App\Models\User;
use App\Notifications\ResetPasswordNotification;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\Cache;
use Illuminate\Support\Facades\Hash;

class PasswordResetLinkController extends Controller
{
    public function store(Request $request): JsonResponse
    {
        $request->validate([
            'email' => ['required', 'email'],
        ]);

        $user = User::where('email', $request->email)->first();

        if (!$user) {
            return response()->json([
                'message' => "You don't have an account. Please register first.",
            ], 404);
        }

        $otp = (string) random_int(1000, 9999);

        $cacheKey = 'password_reset_' . $user->email;

        Cache::put(
            $cacheKey,
            ['otp' => Hash::make($otp)],
            now()->addMinutes(3)
        );

        $user->notify(new ResetPasswordNotification($otp));

        return response()->json([
            'message' => 'OTP sent to your email. Please verify to reset your password.',
        ], 200);
    }
}
