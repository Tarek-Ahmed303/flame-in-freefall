<?php

namespace App\Http\Controllers;

use Illuminate\Http\Request;
use App\Http\Requests\UpdateFcmTokenRequest;
class UpdateFcmTokenController extends Controller
{
    //
    public function update(UpdateFcmTokenRequest $request){
        $user=$request->user();
        $fcm=$request->validated('fcm_token');
        $user->update([
            'fcm_token'=>$fcm
        ]);
        return response()->json([
            'message'=>'fcm token updated'
        ]);
    }
}
