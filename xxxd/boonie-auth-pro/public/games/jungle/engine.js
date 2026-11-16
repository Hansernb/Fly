/*!
 * 森林斗兽棋 · 规则引擎（独立、零依赖）
 * ---------------------------------------------------------------
 * 浏览器：window.JungleEngine
 * Node ：const E = require('./engine.js')   // 用于跑校验脚本
 *
 * 棋盘：7 列 × 9 行（col 0..6，row 0..8）
 *   上方 row 0 是蓝方兽穴，下方 row 8 是红方兽穴
 *   河流：row 3/4/5 的 col 1,2,4,5（col 3 为陆桥）
 *   陷阱：蓝方 (2,0)(4,0)(3,1)，红方 (2,8)(4,8)(3,7)
 *
 * 规则：
 *   1. 棋子向上下左右走一格，不能进己方兽穴；
 *   2. 大吃小（战力 1..8），同级同归于尽；鼠可吃象，象不能吃鼠；
 *   3. 只有鼠能下水；水中鼠不能吃陆地棋，陆地棋（含象）也吃不了水中鼠；
 *   4. 狮/虎可横竖跳过整段河，但河道上只要有鼠就跳不过；
 *   5. 站在敌方陷阱里的棋子战力视为 0（可被任意敌子吃，也吃不动别人）；
 *   6. 进对方兽穴 / 吃光对方 / 对方无子可走 → 获胜。
 */
(function (root, factory) {
  const api = factory()
  if (typeof module === 'object' && module.exports) module.exports = api
  else root.JungleEngine = api
})(typeof globalThis !== 'undefined' ? globalThis : this, function () {
  'use strict'

  const COLS = 7
  const ROWS = 9

  const ANIMALS = {
    rat: { name: '鼠', emoji: '🐭', rank: 1, value: 45 },
    cat: { name: '猫', emoji: '🐱', rank: 2, value: 20 },
    dog: { name: '狗', emoji: '🐶', rank: 3, value: 30 },
    wolf: { name: '狼', emoji: '🐺', rank: 4, value: 45 },
    monkey: { name: '猴', emoji: '🐵', rank: 5, value: 55 },
    tiger: { name: '虎', emoji: '🐯', rank: 6, value: 90 },
    lion: { name: '狮', emoji: '🦁', rank: 7, value: 100 },
    elephant: { name: '象', emoji: '🐘', rank: 8, value: 115 }
  }

  const DEN = { red: { c: 3, r: 8 }, blue: { c: 3, r: 0 } }
  const TRAP = {
    red: [{ c: 2, r: 8 }, { c: 4, r: 8 }, { c: 3, r: 7 }],
    blue: [{ c: 2, r: 0 }, { c: 4, r: 0 }, { c: 3, r: 1 }]
  }
  const WATER_ROWS = [3, 4, 5]
  const WATER_COLS = [1, 2, 4, 5]
  const DIRS = [
    { dc: 0, dr: -1 },
    { dc: 0, dr: 1 },
    { dc: -1, dr: 0 },
    { dc: 1, dr: 0 }
  ]

  /* ------------------------------ 地形 ------------------------------ */

  function inBoard(c, r) {
    return c >= 0 && c < COLS && r >= 0 && r < ROWS
  }

  function isWater(c, r) {
    return WATER_ROWS.indexOf(r) >= 0 && WATER_COLS.indexOf(c) >= 0
  }

  function terrainAt(c, r) {
    if (!inBoard(c, r)) return null
    if (c === DEN.red.c && r === DEN.red.r) return 'den-red'
    if (c === DEN.blue.c && r === DEN.blue.r) return 'den-blue'
    if (TRAP.red.some((t) => t.c === c && t.r === r)) return 'trap-red'
    if (TRAP.blue.some((t) => t.c === c && t.r === r)) return 'trap-blue'
    if (isWater(c, r)) return 'water'
    return 'land'
  }

  /** 该格是不是 side 的对家陷阱（进来战力归零） */
  function isEnemyTrap(side, c, r) {
    const t = terrainAt(c, r)
    return t === (side === 'red' ? 'trap-blue' : 'trap-red')
  }

  /* ------------------------------ 状态 ------------------------------ */

  const INITIAL_LAYOUT = [
    // 蓝方（上）：狮虎守两角，鼠象压边，狗猫狼猴居中
    ['blue', 'lion', 0, 0],
    ['blue', 'tiger', 6, 0],
    ['blue', 'dog', 1, 1],
    ['blue', 'cat', 5, 1],
    ['blue', 'rat', 0, 2],
    ['blue', 'wolf', 2, 2],
    ['blue', 'monkey', 4, 2],
    ['blue', 'elephant', 6, 2],
    // 红方（下）：与上方左右镜像
    ['red', 'tiger', 0, 8],
    ['red', 'lion', 6, 8],
    ['red', 'cat', 1, 7],
    ['red', 'dog', 5, 7],
    ['red', 'elephant', 0, 6],
    ['red', 'wolf', 2, 6],
    ['red', 'monkey', 4, 6],
    ['red', 'rat', 6, 6]
  ]

  /** 由 [[side, type, c, r], ...] 造一个初始状态 */
  function createState(layout) {
    const src = layout || INITIAL_LAYOUT
    const pieces = src.map((row, i) => ({
      id: row[0].charAt(0) + i,
      side: row[0],
      type: row[1],
      c: row[2],
      r: row[3]
    }))
    return { pieces: pieces, turn: 'red', ply: 0 }
  }

  function createInitialState() {
    return createState(null)
  }

  function cloneState(state) {
    return {
      pieces: state.pieces.map((p) => ({ id: p.id, side: p.side, type: p.type, c: p.c, r: p.r })),
      turn: state.turn,
      ply: state.ply || 0
    }
  }

  function pieceAt(state, c, r) {
    for (let i = 0; i < state.pieces.length; i++) {
      const p = state.pieces[i]
      if (p.c === c && p.r === r) return p
    }
    return null
  }

  function otherSide(side) {
    return side === 'red' ? 'blue' : 'red'
  }

  /* ------------------------------ 战斗力 ------------------------------ */

  function attackRank(piece, c, r) {
    return isEnemyTrap(piece.side, c, r) ? 0 : ANIMALS[piece.type].rank
  }

  function defendRank(piece, c, r) {
    return isEnemyTrap(piece.side, c, r) ? 0 : ANIMALS[piece.type].rank
  }

  /**
   * 攻击方能否吃掉（或同归于尽）defender
   * @returns {'capture'|'mutual'|null}
   */
  function captureOutcome(state, attacker, defender, toC, toR) {
    if (attacker.side === defender.side) return null
    const atkInWater = isWater(attacker.c, attacker.r)
    const defInWater = isWater(toC, toR)

    // 水中鼠：只能被水中鼠吃，且只能吃水中的对手
    if (defInWater && defender.type === 'rat' && !(attacker.type === 'rat' && atkInWater)) return null
    if (atkInWater && attacker.type === 'rat' && !defInWater) return null

    // 象吃不了鼠（鼠能吃象）
    if (attacker.type === 'elephant' && defender.type === 'rat') return null
    if (attacker.type === 'rat' && defender.type === 'elephant') return 'capture'

    const a = attackRank(attacker, attacker.c, attacker.r)
    const d = defendRank(defender, toC, toR)
    if (a > d) return 'capture'
    if (a === d) return 'mutual'
    return null
  }

  /* ------------------------------ 走法 ------------------------------ */

  function pushMove(state, piece, c, r, moves, jump) {
    if (!inBoard(c, r)) return
    const terrain = terrainAt(c, r)
    if (terrain === 'den-' + piece.side) return // 不能进己方兽穴
    if (terrain === 'water' && piece.type !== 'rat') return
    const target = pieceAt(state, c, r)
    if (target) {
      const outcome = captureOutcome(state, piece, target, c, r)
      if (!outcome) return
      moves.push({
        pieceId: piece.id,
        from: { c: piece.c, r: piece.r },
        to: { c: c, r: r },
        captureId: target.id,
        captureType: target.type,
        mutual: outcome === 'mutual',
        jump: !!jump
      })
      return
    }
    moves.push({
      pieceId: piece.id,
      from: { c: piece.c, r: piece.r },
      to: { c: c, r: r },
      captureId: null,
      captureType: null,
      mutual: false,
      jump: !!jump
    })
  }

  function legalMoves(state, side) {
    const who = side || state.turn
    const moves = []
    for (let i = 0; i < state.pieces.length; i++) {
      const p = state.pieces[i]
      if (p.side !== who) continue

      for (let d = 0; d < DIRS.length; d++) {
        const dir = DIRS[d]
        pushMove(state, p, p.c + dir.dc, p.r + dir.dr, moves, false)
      }

      // 狮 / 虎 跳河：越过整段水面落到对岸，河道上有鼠则跳不过
      if (p.type === 'tiger' || p.type === 'lion') {
        for (let d = 0; d < DIRS.length; d++) {
          const dir = DIRS[d]
          let c = p.c + dir.dc
          let r = p.r + dir.dr
          if (!inBoard(c, r) || !isWater(c, r)) continue
          let blocked = false
          while (inBoard(c, r) && isWater(c, r)) {
            const occ = pieceAt(state, c, r)
            if (occ && occ.type === 'rat') blocked = true
            c += dir.dc
            r += dir.dr
          }
          if (blocked || !inBoard(c, r)) continue
          pushMove(state, p, c, r, moves, true)
        }
      }
    }
    return moves
  }

  function movesOfPiece(state, pieceId) {
    return legalMoves(state, state.turn).filter((m) => m.pieceId === pieceId)
  }

  /* ------------------------------ 执行走子 ------------------------------ */

  function applyMove(state, move) {
    const pieces = state.pieces.map((p) => ({ id: p.id, side: p.side, type: p.type, c: p.c, r: p.r }))
    const mover = state.turn
    const enemy = otherSide(mover)
    let captured = null
    let mutual = false

    const ai = pieces.findIndex((p) => p.id === move.pieceId)
    if (ai < 0) throw new Error('棋子不存在: ' + move.pieceId)

    if (move.captureId) {
      const di = pieces.findIndex((p) => p.id === move.captureId)
      captured = pieces[di] ? { type: pieces[di].type, side: pieces[di].side } : null
      mutual = !!move.mutual
      pieces.splice(di, 1)
      const mi = pieces.findIndex((p) => p.id === move.pieceId)
      if (mutual) {
        pieces.splice(mi, 1)
      } else {
        pieces[mi].c = move.to.c
        pieces[mi].r = move.to.r
      }
    } else {
      pieces[ai].c = move.to.c
      pieces[ai].r = move.to.r
    }

    const next = { pieces: pieces, turn: enemy, ply: (state.ply || 0) + 1 }
    let winner = null

    // 1) 进对方兽穴
    if (!mutual && move.to.c === DEN[enemy].c && move.to.r === DEN[enemy].r) winner = mover
    // 2) 吃光对方
    if (!winner && !pieces.some((p) => p.side === enemy)) winner = mover
    // 3) 对方无子可走（困毙）
    if (!winner && legalMoves(next, enemy).length === 0) winner = mover

    if (winner) {
      next.winner = winner
      next.turn = enemy
    }
    return { state: next, captured: captured, mutual: mutual, winner: winner }
  }

  /* ------------------------------ 局面评估 ------------------------------ */

  function distTo(a, b) {
    return Math.abs(a.c - b.c) + Math.abs(a.r - b.r)
  }

  /** 以 me 视角评估局面（越大越好） */
  function evaluate(state, me) {
    const enemy = otherSide(me)
    let score = 0

    for (let i = 0; i < state.pieces.length; i++) {
      const p = state.pieces[i]
      score += (p.side === me ? 1 : -1) * ANIMALS[p.type].value
    }

    for (let i = 0; i < state.pieces.length; i++) {
      const p = state.pieces[i]
      if (p.side === me) {
        // 越靠近对方兽穴越好（能进穴直接赢）
        score += Math.max(0, 12 - distTo(p, DEN[enemy])) * 2.6
        // 敌方兽穴门口（陷阱区）有子额外加分，但站敌方陷阱里战力归零要扣
        if (isEnemyTrap(me, p.c, p.r)) score -= ANIMALS[p.type].value * 0.28
      } else {
        // 对方越靠近我方兽穴，越危险
        score -= Math.max(0, 12 - distTo(p, DEN[me])) * 3.4
        if (isEnemyTrap(enemy, p.c, p.r)) score += ANIMALS[p.type].value * 0.18
      }
    }

    score += (legalMoves(state, me).length - legalMoves(state, enemy).length) * 1.6
    return score
  }

  /* ------------------------------ AI ------------------------------ */

  function chooseNormal(state, me, moves) {
    const enemy = otherSide(me)
    let best = null
    let bestScore = -Infinity

    for (let i = 0; i < moves.length; i++) {
      const m = moves[i]
      const res = applyMove(state, m)
      if (res.winner === me) return m // 直接赢，立刻走

      let score = 0
      if (res.captured) score += ANIMALS[res.captured.type].value * (res.mutual ? 0.2 : 1)

      // 一步之后，对方能白吃我多少
      const replies = legalMoves(res.state, enemy)
      let worst = 0
      for (let j = 0; j < replies.length; j++) {
        const rp = replies[j]
        if (!rp.captureId) continue
        const victim = res.state.pieces.find((p) => p.id === rp.captureId)
        if (!victim) continue
        const gain = ANIMALS[victim.type].value * (rp.mutual ? 0.25 : 1)
        if (gain > worst) worst = gain
      }
      score -= worst * 0.9
      if (res.winner === enemy) score -= 10000

      const moved = res.state.pieces.find((p) => p.id === m.pieceId)
      if (moved) score += (12 - distTo(moved, DEN[enemy])) * 0.7
      score += Math.random() * 2

      if (score > bestScore) {
        bestScore = score
        best = m
      }
    }
    return best || moves[0]
  }

  function chooseHard(state, me, moves) {
    const enemy = otherSide(me)
    let best = null
    let bestVal = -Infinity

    for (let i = 0; i < moves.length; i++) {
      const m = moves[i]
      const res = applyMove(state, m)
      if (res.winner === me) return m

      const replies = legalMoves(res.state, enemy)
      if (!replies.length) return m

      // 对方挑对我最不利的一手（两层搜索）
      let worst = Infinity
      for (let j = 0; j < replies.length; j++) {
        const r2 = applyMove(res.state, replies[j])
        if (r2.winner === enemy) {
          worst = -10000
          break
        }
        const v = evaluate(r2.state, me)
        if (v < worst) worst = v
      }

      // 略微偏好即时吃子，避免同分时乱选
      let bonus = 0
      if (res.captured) bonus += ANIMALS[res.captured.type].value * (res.mutual ? 0.15 : 0.6)
      const val = worst + bonus + Math.random() * 0.8

      if (val > bestVal) {
        bestVal = val
        best = m
      }
    }
    return best || moves[0]
  }

  /**
   * 选择一步棋
   * @param {object} state
   * @param {'easy'|'normal'|'hard'} level
   */
  function chooseMove(state, level) {
    const me = state.turn
    const moves = legalMoves(state, me)
    if (!moves.length) return null
    if (level === 'easy') return moves[Math.floor(Math.random() * moves.length)]
    if (level === 'hard') return chooseHard(state, me, moves)
    return chooseNormal(state, me, moves)
  }

  /* ------------------------------ 残局关卡 ------------------------------ */
  /* pieces: [side, type, c, r]；limit：红方（玩家）限定步数
     每关都是红先，蓝方由困难 AI 防守 */

  const LEVELS = [
    {
      id: 1,
      name: '双卫守穴',
      limit: 5,
      tips: '猫躲在陷阱里战力归零，但旁边的狼还盯着它——想清楚先吃谁。',
      pieces: [
        ['red', 'lion', 3, 2],
        ['blue', 'wolf', 2, 1],
        ['blue', 'cat', 3, 1]
      ]
    },
    {
      id: 2,
      name: '虎跃清溪',
      limit: 6,
      tips: '正面过不去就跳过河，落点上的敌子也能一口吞下。',
      pieces: [
        ['red', 'tiger', 1, 6],
        ['blue', 'cat', 1, 2],
        ['blue', 'dog', 2, 1]
      ]
    },
    {
      id: 3,
      name: '陷阱迷局',
      limit: 6,
      tips: '踏进敌方陷阱战力就变 0，守穴的猫没清干净前别乱进。',
      pieces: [
        ['red', 'lion', 3, 6],
        ['blue', 'cat', 3, 2],
        ['blue', 'wolf', 2, 2]
      ]
    },
    {
      id: 4,
      name: '鼠象猫狗',
      limit: 5,
      tips: '大象只怕老鼠；猫狗怕狮子——分工要分好。',
      pieces: [
        ['red', 'rat', 1, 2],
        ['red', 'lion', 3, 2],
        ['blue', 'elephant', 2, 2],
        ['blue', 'cat', 2, 1],
        ['blue', 'dog', 3, 1]
      ]
    },
    {
      id: 5,
      name: '双将守门',
      limit: 6,
      tips: '两个看门的挡在穴前，先吃哪个决定成败。',
      pieces: [
        ['red', 'lion', 3, 6],
        ['red', 'monkey', 4, 6],
        ['blue', 'wolf', 3, 3],
        ['blue', 'dog', 2, 2]
      ]
    },
    {
      id: 6,
      name: '河中鼠影',
      limit: 7,
      tips: '河里有鼠，狮子就跳不过去——先把这只老鼠处理掉。',
      pieces: [
        ['red', 'lion', 1, 6],
        ['red', 'rat', 6, 6],
        ['blue', 'rat', 2, 4],
        ['blue', 'cat', 1, 1]
      ]
    },
    {
      id: 7,
      name: '虎口夺穴',
      limit: 8,
      tips: '对手已经压到中场，慢一步就来不及，必须抢攻。',
      pieces: [
        ['red', 'tiger', 3, 3],
        ['red', 'dog', 2, 6],
        ['blue', 'wolf', 3, 5],
        ['blue', 'cat', 2, 2],
        ['blue', 'monkey', 4, 2]
      ]
    }
  ]

  return {
    COLS: COLS,
    ROWS: ROWS,
    ANIMALS: ANIMALS,
    DEN: DEN,
    TRAP: TRAP,
    INITIAL_LAYOUT: INITIAL_LAYOUT,
    LEVELS: LEVELS,
    terrainAt: terrainAt,
    isWater: isWater,
    isEnemyTrap: isEnemyTrap,
    inBoard: inBoard,
    createState: createState,
    createInitialState: createInitialState,
    cloneState: cloneState,
    pieceAt: pieceAt,
    otherSide: otherSide,
    legalMoves: legalMoves,
    movesOfPiece: movesOfPiece,
    applyMove: applyMove,
    evaluate: evaluate,
    chooseMove: chooseMove
  }
})
