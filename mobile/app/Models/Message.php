<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Model;

class Message extends Model
{
    //
    protected $fillable=[
        'user_id','answer','message','chat_id'
    ];

    public function casts():array{
        return [
'answer'=>'array'
        ];
    }
    public function chat()
{
    return $this->belongsTo(Chat::class);
}
    public function user(){
        return $this->belongsTo(User::class);
    }
}
