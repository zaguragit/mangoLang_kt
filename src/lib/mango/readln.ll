
define i8* @readln() noinline nounwind optnone uwtable "correctly-rounded-divide-sqrt-fp-math"="false" "disable-tail-calls"="false" "less-precise-fpmad"="false" "no-frame-pointer-elim"="true" "no-frame-pointer-elim-non-leaf" "no-infs-fp-math"="false" "no-jump-tables"="false" "no-nans-fp-math"="false" "no-signed-zeros-fp-math"="false" "no-trapping-math"="false" "stack-protector-buffer-size"="8" "target-cpu"="x86-64" "target-features"="+fxsr,+mmx,+sse,+sse2,+x87" "unsafe-fp-math"="false" "use-soft-float"="false" {
  %1 = alloca i32, align 4
  %2 = alloca i8*, align 8
  %3 = alloca i8, align 1
  %4 = call i8* @malloc(i64 256)
  store i8* %4, i8** %2, align 8
  store i8 0, i8* %3, align 1
  br label %5

; <label>:5:                                      ; preds = %17, %0
  %6 = call i32 @getchar()
  store i32 %6, i32* %1, align 4
  %7 = icmp ne i32 %6, 10
  br i1 %7, label %8, label %15

; <label>:8:                                      ; preds = %5
  %9 = load i32, i32* %1, align 4
  %10 = icmp ne i32 %9, -1
  br i1 %10, label %11, label %15

; <label>:11:                                     ; preds = %8
  %12 = load i8, i8* %3, align 1
  %13 = zext i8 %12 to i32
  %14 = icmp ne i32 %13, 255
  br label %15

; <label>:15:                                     ; preds = %11, %8, %5
  %16 = phi i1 [ false, %8 ], [ false, %5 ], [ %14, %11 ]
  br i1 %16, label %17, label %25

; <label>:17:                                     ; preds = %15
  %18 = load i32, i32* %1, align 4
  %19 = trunc i32 %18 to i8
  %20 = load i8*, i8** %2, align 8
  %21 = load i8, i8* %3, align 1
  %22 = add i8 %21, 1
  store i8 %22, i8* %3, align 1
  %23 = zext i8 %21 to i64
  %24 = getelementptr inbounds i8, i8* %20, i64 %23
  store i8 %19, i8* %24, align 1
  br label %5

; <label>:25:                                     ; preds = %15
  %26 = load i8*, i8** %2, align 8
  %27 = load i8, i8* %3, align 1
  %28 = zext i8 %27 to i64
  %29 = getelementptr inbounds i8, i8* %26, i64 %28
  store i8 0, i8* %29, align 1
  %30 = load i8*, i8** %2, align 8
  %31 = load i8, i8* %3, align 1
  %32 = zext i8 %31 to i32
  %33 = add nsw i32 %32, 1
  %34 = sext i32 %33 to i64
  %35 = call i8* @realloc(i8* %30, i64 %34)
  ret i8* %35
}

declare i8* @malloc(i64) #0
declare i8* @realloc(i8*, i64) #0
declare i32 @getchar() #0

attributes #0 = { "correctly-rounded-divide-sqrt-fp-math"="false" "disable-tail-calls"="false" "less-precise-fpmad"="false" "no-frame-pointer-elim"="true" "no-frame-pointer-elim-non-leaf" "no-infs-fp-math"="false" "no-nans-fp-math"="false" "no-signed-zeros-fp-math"="false" "no-trapping-math"="false" "stack-protector-buffer-size"="8" "target-cpu"="x86-64" "target-features"="+fxsr,+mmx,+sse,+sse2,+x87" "unsafe-fp-math"="false" "use-soft-float"="false" }