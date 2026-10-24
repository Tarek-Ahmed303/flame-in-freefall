<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Model;

class Notification extends Model
{
    //
    protected $fillable=[
        'body','title','user_id'
    ]
     public function users(){
        return $this->belongsTo(User::class);
    }
}
