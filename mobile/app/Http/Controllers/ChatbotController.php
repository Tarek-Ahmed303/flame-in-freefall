<?php

namespace App\Http\Controllers;
use App\Http\Requests\SendMessageRequest;
use Illuminate\Support\Facades\Http;
use Illuminate\Http\Request;
use App\Models\Message;
use Illuminate\Http\JsonResponse;
use App\Models\Chat;
class ChatbotController extends Controller
{
    //
    public function getmessages(Request $request):JsonResponse{
        $user=$request->user();
        $request->validate([
        'chat_id' => ['required', 'integer'],
    ]);
    $chat = Chat::where('user_id', $user->id)
    ->with('messages')
    ->findOrFail($request->chat_id);
        
return response()->json(
    [
        
    "message"=>"Your Chat History is Retrieved",
    'chat' => $chat,
'data' => $chat->messages
    ]
);
    }
    public function sendmessages(SendMessageRequest $request):JsonResponse{
$user=$request->user();
$chatid=$request->validated('chat_id');
$question=$request->validated('question');
if($chatid){
    $chat=Chat::where('user_id',$user->id)->findOrFail($chatid);

}
else{
    $chat=Chat::create([
        'user_id'=>$user->id,
        'title'=>null
    ]);
}

$response=Http::timeout(60)->post(config('services.ai-services.url').'/ask',
[
    "question"=>$request->validated('question')

]);
$response->throw();
$answer=$response->json();

  if ($chat->title===null) {
        $chat->update([
            'title' => mb_substr($question, 0, 50),
        ]);
    }

Message::create([
    'user_id' => $user->id,
    'message' => $question,
    'answer' => $answer,
    'chat_id' => $chat->id,
]);
return response()->json([
        "message"=> "Chatbot response generated successfully.",
        'chat_id' => $chat->id,
        'title' => $chat->title,
            "data" =>[
                'answer'=>$answer,
                
            ]
        ], 201);

    }

    
}
