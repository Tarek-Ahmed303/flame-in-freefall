<?php

namespace App\Http\Controllers\Auth;

use App\Http\Controllers\Controller;
use App\Models\User;
use Illuminate\Auth\Events\Registered;
use Illuminate\Http\Request;
use Illuminate\Http\JsonResponse;
use Illuminate\Support\Facades\Hash;
use Illuminate\Validation\Rules;
use Illuminate\Validation\ValidationException;
use Illuminate\Support\Facades\Mail;
use App\Mail\SendRegistrationOtpMail;
use Illuminate\Support\Str;
use Illuminate\Support\Facades\Cache;
use App\Enums\AssignedModule;
use Illuminate\Validation\Rule;
class RegisteredUserController extends Controller
{
    /**
     * Handle an incoming registration request.
     *
     * @throws ValidationException
     */
    public function store(Request $request): JsonResponse
    {
    
       $data= $request->validate([
            'name' => ['required', 'string', 'max:255'],
            'email' => ['required', 'string', 'lowercase', 'email', 'max:255', 'unique:'.User::class],
            'password' => ['required', 'confirmed', Rules\Password::defaults(),'min:10'],
            'assigned_module'=>['required','string',Rule::enum(AssignedModule::class)]
        ]);
        $data['password'] = Hash::make($data['password']);

        unset($data['password_confirmation']);
         
        $otp = (string) random_int(1000, 9999);
$verificationId = Str::uuid()->toString();
$cacheKey = 'registration_' . $verificationId;

        Cache::put($cacheKey, ['userdata' => $data, 'otp'  => Hash::make($otp)], now()->addMinutes(3));
       
  Mail::to($data['email'])->send(new SendRegistrationOtpMail($otp));
 
        

        return response()->json([
             "message"=>"OTP sent to your email. Please verify to complete registration.",
    "verification_id"=>$verificationId 
        ],201);
    }
}
