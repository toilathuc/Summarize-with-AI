**BÁO CÁO BÀI TẬP LỚN**
1. # **Thông tin chung**
- Nhóm:….
- Thành viên: Nguyễn Văn A (22000123), Trần Văn B (23000111)
- Ứng dụng: Hệ thống thương mại điện tử
- Repo gốc: <https://github>... (repo phiên bản có từ trước)
- Repo bản cải tiến: <https://github>... (repo bản cuối)
1. # **Thông tin chi tiết**

|**STT**|**Vấn đề**|**Giải pháp**|**Kết quả**|**Minh chứng**|
| :-: | :-: | :-: | :-: | :-: |
|1|Các thành phần trong hệ thống kết dính cao, khó thay đổi|Mô đun hóa|Hệ thống được chia thành các mô đun, mức độ phụ thuộc giữa các mô đun (coupling) giảm, mức độ tập trung chức năng (cohesion) cao  |Lớp Customer (Customer.py)|
|2|Hệ thống gọi API của bên thứ 3, API này không ổn định |Áp dụng Circuit breaker|Hạn chế được số lần gọi API khi API đang không ổn định|<p>Lớp CircuitBreaker (CB.py)</p><p>Phương thức exchange() trong (Transaction.py)</p>|
|…|||||


