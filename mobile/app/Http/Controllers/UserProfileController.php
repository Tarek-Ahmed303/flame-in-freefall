<?php

namespace App\Http\Controllers;

use Illuminate\Http\Request;

class UserProfileController extends Controller
{
    //
    public function show(Request $request){
        $user=$request->user();
        return response()->json([
            'name'=>$user->name,
            'module'=>$user->assigned_module?->value,
            'email'=>$user->email
        ]);
    }
}
