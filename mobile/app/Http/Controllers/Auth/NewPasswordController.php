
<?php

namespace App\Http\Controllers\Auth;

use App\Http\Controllers\Controller;
use App\Models\User;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\Cache;
use Illuminate\Support\Facades\Hash;
use Illuminate\Support\Str;
use Illuminate\Validation\Rules;

class NewPasswordController extends Controller
{
    public function store(Request $request): JsonResponse
    {
        $request->validate([
            'otp' => ['required', 'digits:4'],
            'email' => ['required', 'email'],
            'password' => ['required', 'confirmed', Rules\Password::defaults()],
        ]);

        $user = User::where('email', $request->email)->first();

        if (!$user) {
            return response()->json([
                'message' => "You don't have an account. Please register first.",
            ], 404);
        }

        $cacheKey = 'password_reset_' . $user->email;

        $data = Cache::get($cacheKey);

        if (!$data) {
            return response()->json([
                'message' => 'Verification code expired. Please request a new code.',
            ], 422);
        }

        if (!Hash::check($request->otp, $data['otp'])) {
            return response()->json([
                'message' => 'Incorrect code.',
            ], 422);
        }

        $user->forceFill([
            'password' => Hash::make($request->password),
            'remember_token' => Str::random(60),
        ])->save();

        Cache::forget($cacheKey);

        return response()->json([
            'message' => 'Password has been reset successfully.',
        ], 200);
    }
}
