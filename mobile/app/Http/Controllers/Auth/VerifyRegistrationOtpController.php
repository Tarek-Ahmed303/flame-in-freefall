<?php

namespace App\Http\Controllers\Auth;

use App\Http\Controllers\Controller;
use App\Models\User;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\Cache;
use Illuminate\Support\Facades\Hash;

class VerifyRegistrationOtpController extends Controller
{
    public function store(Request $request)
    {
        $request->validate([
            'otp' => ['required', 'digits:4'],
            'verification_id' => ['required', 'uuid'],
        ]);

        $cacheKey = 'registration_' . $request->verification_id;

        $data = Cache::get($cacheKey);

        if (!$data) {
            return response()->json([
                'message' => 'Verification code expired. Please register again.',
            ], 422);
        }

        if (!Hash::check($request->otp, $data['otp'])) {
            return response()->json([
                'message' => 'Incorrect code.',
            ], 422);
        }

        $user = User::create($data['userdata']);

        $user->email_verified_at = now();
        $user->save();

        $token = $user->createToken('authtoken')->plainTextToken;

        Cache::forget($cacheKey);

        return response()->json([
            'message' => 'Registration completed successfully.',
            'token' => $token,
            'user' => $user,
        ], 201);
    }
}
