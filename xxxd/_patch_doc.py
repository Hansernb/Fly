import io
p = 'D:/workSpace/Fly/xxxd/springBootTest/AI-CONTEXT.md'
s = io.open(p, encoding='utf-8').read()

def rep(old, new):
    global s
    assert s.count(old) == 1, old[:60]
    s = s.replace(old, new, 1)

rep("""- 表：`boonie_member`（phone/nickname/avatar/total_points/cum_points/level_name，phone 唯一）""",
"""- **会员身份 = 登录账号**（2026-11-11 改）：`boonie_member.user_id` 绑定 `boonie_user.id`（唯一），哪个账号买票积分就加到该账号的卡上，**与取票人手机号无关**；`phone` 改为可空、仅作展示/游客兜底（账号会员记录最近一次下单手机号）
- 表：`boonie_member`（user_id/phone/nickname/avatar/total_points/cum_points/level_name，user_id 唯一、phone 唯一且可空）""")

rep("""- 规则：**手机号下单即自动开通会员**（无需注册/登录，游客也可下单）；""",
"""- 规则：**登录账号下单即自动开通会员**（`getOrCreateForUser`：先按 user_id 找 → 再尝试把同手机号的游客档案归并到该账号 → 都没有才新建）；未登录游客仍按手机号建档（`getOrCreate`），但**若该手机号已绑定账号则拒绝下单**（400「该手机号已绑定会员账号，请先登录」，防止冒用他人手机号花积分）；""")

rep("""- 接口：
  - `GET /api/member?phone=` — 会员信息 + 积分明细 logs（不存在返回 404，前端降级本地）""",
"""- 接口（都支持 `Authorization: Bearer`，带令牌一律按账号查，忽略 phone）：
  - `GET /api/member?phone=` — 会员信息 + 积分明细 logs（不存在返回 404，前端降级本地）""")

io.open(p, 'w', encoding='utf-8', newline='\n').write(s)
print('ok')
