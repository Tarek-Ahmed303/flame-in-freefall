<?php

namespace App\Http\Controllers;

use App\Models\Chat;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\Auth;

class ChatController extends Controller
{
    public function index(Request $request)
    {
        $user=$request->user();
        $chats = Chat::where('user_id',$user->id)
            ->latest() ->get();

        return response()->json([
            'chats' => $chats,
        ]);
    }

    public function store(Request $request)
    {
        $user=$request->user();
        $chat = Chat::create([
            'user_id' => $user->id,
            'title' => null,
        ]);

        return response()->json([
            'message' => 'Chat created successfully',
            'chat' => $chat,
        ], 201);
    }

    public function show(Request $request,$id)
    {
        $user=$request->user();
        $chat = Chat::where('user_id',$user->id)
            ->with('messages')
            ->findOrFail($id);

        return response()->json([
            'chat' => $chat,
        ]);
    }
}
